// https://searchcode.com/api/result/109544216/

package org.atlasapi.feeds.tasks.checking;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Set;

import org.atlasapi.feeds.tasks.Status;
import org.atlasapi.feeds.tasks.Task;
import org.atlasapi.feeds.tasks.Destination.DestinationType;
import org.atlasapi.feeds.tasks.persistence.TaskStore;
import org.atlasapi.feeds.tasks.youview.processing.TaskProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableSet;
import com.metabroadcast.common.scheduling.ScheduledTask;
import com.metabroadcast.common.scheduling.UpdateProgress;


public class RemoteCheckTask extends ScheduledTask {
    
    private static final Set<Status> TO_BE_CHECKED = ImmutableSet.of(
            Status.ACCEPTED,
            Status.VALIDATING,
            Status.QUARANTINED,
            Status.COMMITTING,
            Status.COMMITTED,
            Status.PUBLISHING
    );
    
    private final Logger log = LoggerFactory.getLogger(RemoteCheckTask.class);
    private final TaskStore taskStore;
    private final TaskProcessor processor;
    private final DestinationType destinationType;

    public RemoteCheckTask(TaskStore taskStore, TaskProcessor processor, DestinationType destinationType) {
        this.taskStore = checkNotNull(taskStore);
        this.processor = checkNotNull(processor);
        this.destinationType = checkNotNull(destinationType);
    }

    @Override
    protected void runTask() {
        UpdateProgress progress = UpdateProgress.START;
        for (Status uncheckedStatus : TO_BE_CHECKED) {
            Iterable<Task> tasksToCheck = taskStore.allTasks(destinationType, uncheckedStatus);
            for (Task task : tasksToCheck) {
                if (!shouldContinue()) {
                    break;
                }
                try {
                    processor.checkRemoteStatusOf(task);
                    progress = progress.reduce(UpdateProgress.SUCCESS);
                } catch (Exception e) {
                    log.error("error checking task {}", task.id(), e);
                    progress = progress.reduce(UpdateProgress.FAILURE);
                }
                reportStatus(progress.toString());
            }
        }
    }
}

