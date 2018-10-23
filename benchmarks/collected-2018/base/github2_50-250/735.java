// https://searchcode.com/api/result/109548029/

package org.atlasapi.remotesite.redux;

import static org.atlasapi.persistence.logging.AdapterLogEntry.errorEntry;
import static org.atlasapi.persistence.logging.AdapterLogEntry.infoEntry;

import java.util.List;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.atlasapi.media.entity.Item;
import org.atlasapi.persistence.content.ContentWriter;
import org.atlasapi.persistence.logging.AdapterLog;
import org.atlasapi.remotesite.SiteSpecificAdapter;
import org.atlasapi.remotesite.redux.ReduxDayUpdateTask.Builder;
import org.joda.time.LocalDate;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.metabroadcast.common.scheduling.ScheduledTask;
import com.metabroadcast.common.scheduling.UpdateProgress;
import com.metabroadcast.common.time.DateTimeZones;

public class ScheduledReduxDayUpdateTask extends ScheduledTask {

    private final ExecutorService executor;
    private final AdapterLog log;

    private final Builder taskBuilder;

    public ScheduledReduxDayUpdateTask(ReduxClient client, ContentWriter writer, SiteSpecificAdapter<Item> adapter, AdapterLog log, ExecutorService executor) {
        this.log = log;
        this.executor = executor;
        this.taskBuilder = ReduxDayUpdateTask.dayUpdateTaskBuilder(client, writer, adapter, log);
    }
    
    public ScheduledReduxDayUpdateTask(ReduxClient client, ContentWriter writer, SiteSpecificAdapter<Item> adapter, AdapterLog log) {
        this(client, writer, adapter, log, new ThreadPoolExecutor(0, 10, 1, TimeUnit.MINUTES, new LinkedBlockingDeque<Runnable>(), new ThreadFactoryBuilder().setNameFormat("Redux Updater %d").build()));
    }
    
    @Override
    protected void runTask() {
        //TODO: What days do we actually want to iterate over?
        Iterable<LocalDate> days = ImmutableList.of(new LocalDate(DateTimeZones.UTC));
        
        log.record(infoEntry().withSource(getClass()).withDescription("Redux day update started for %s days", Iterables.size(days)));
        reportStatus(String.format("Submitting tasks for %d days", Iterables.size(days)));
        
        final CompletionService<UpdateProgress> completer = new ExecutorCompletionService<UpdateProgress>(executor);
        
        List<Future<UpdateProgress>> submitted = submitTasks(completer, days);
        reportStatus(String.format("Submitted %s tasks", submitted.size()));
        
        UpdateProgress progress = UpdateProgress.START;
        
        for (int i = 0; i < submitted.size(); ) {
            if(!shouldContinue()) {
                reportStatus(String.format("Cancelled. Processed %s/%s tasks. %s", i, submitted.size(), progress));
                cancel(submitted);
                return;
            }
            try {
                Future<UpdateProgress> result = completer.poll(5, TimeUnit.SECONDS);
                if(result != null) {
                    i++;
                    progress = progress.reduce(process(result));
                    reportStatus(String.format("Processed %s/%s tasks. %s", i, submitted.size(), progress));
                }
            } catch (InterruptedException e) {
                log.record(infoEntry().withCause(e).withSource(getClass()).withDescription("Redux update interrupted"));
                reportStatus(String.format("Interrupted. Processed %s/%s tasks. %s", i, submitted.size(), progress));
                cancel(submitted);
                return;
            }
        }
        
        reportStatus(String.format("Finished. Processed %s tasks. %s", submitted.size(), progress));
        log.record(infoEntry().withSource(getClass()).withDescription("Redux day update finished for %s days. %s", Iterables.size(days), progress));
    }

    private UpdateProgress process(Future<UpdateProgress> result) {
        try {
            return result.get();
        } catch (InterruptedException e) {
            return null;
        } catch (ExecutionException e) {
            log.record(errorEntry().withSource(getClass()).withCause(e).withDescription("Exception processing day update task"));
            return null;
        }
    }

    private void cancel(List<Future<UpdateProgress>> submitted) {
        for (Future<UpdateProgress> future : submitted) {
            future.cancel(false);
        }
    }

    private List<Future<UpdateProgress>> submitTasks(final CompletionService<UpdateProgress> completer, Iterable<LocalDate> days) {
        List<Future<UpdateProgress>> results = Lists.newArrayListWithCapacity(Iterables.size(days));
        for (LocalDate day : days) {
            results.add(completer.submit(taskBuilder.updateFor(day)));
            if(!shouldContinue()) {
                break;
            }
        }
        return results;
    }

}

