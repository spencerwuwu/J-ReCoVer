// https://searchcode.com/api/result/109548049/

package org.atlasapi.remotesite.redux;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;


import com.google.common.collect.Lists;
import com.metabroadcast.common.scheduling.Reducible;
import com.metabroadcast.common.scheduling.ScheduledTask;
import com.metabroadcast.common.scheduling.UpdateProgress;

//TODO: split this into more cohesive parts
public class ResultProcessingScheduledTask<T, R extends Reducible<R>> extends ScheduledTask {

    private final ProgressReporter<R> reporter;

    private final Executor executor;
    private final Iterable<Callable<T>> producer;
    private final ResultProcessor<? super T, R> consumer;

    public ResultProcessingScheduledTask(Iterable<Callable<T>> taskProducer, ResultProcessor<? super T, R> taskProcessor, Executor executor) {
        this.producer = taskProducer;
        this.consumer = taskProcessor;
        this.executor = executor;
        this.reporter = new ProgressReporter<R>();
    }

    @Override
    protected final void runTask() {
        
        final CompletionService<T> completer = new ExecutorCompletionService<T>(executor);
        final Semaphore available = new Semaphore(0);
        final List<Future<T>> tasks = Lists.newArrayList();
        final AtomicBoolean producing = new AtomicBoolean(true);

        reporter.reset();
        
        Thread processor = new Thread(new Runnable() {
            @Override
            public void run() {
                processorTasks(completer, available, producing);
                processorRemainingTasks(completer, available);
            }
        }, this.getName() + " Result Processor");
        processor.start();

        produceTasks(completer, available, tasks, producing);
        try {
            processor.join();
        } catch (Exception e) {
            return;
        }
    }

    private void produceTasks(CompletionService<T> completer, Semaphore available, List<Future<T>> tasks, AtomicBoolean producing) {
        Iterator<Callable<T>> taskIterator = producer.iterator();

        while (taskIterator.hasNext() && shouldContinue()) {
            try {
                tasks.add(completer.submit(taskIterator.next()));
                available.release();
                reporter.addSubmission();
            } catch (RejectedExecutionException rje) {
                reporter.addRejection();
            }
            reporter.setProducerStatus("Submitting tasks.");
        }
        
        producing.set(false);

        if (!shouldContinue()) {
            cancelTasks(tasks);
        } else {
            reporter.setProducerStatus("Finished submitting tasks.");
        }
    }

    private void processorTasks(CompletionService<T> results, Semaphore available, AtomicBoolean stillProducing) {
        while (stillProducing.get()) {
            try {
                available.acquire();
                processResult(results.take());
            } catch (InterruptedException ie) {
                return;
            }
        }
    }
    
    private void processorRemainingTasks(CompletionService<T> completer, Semaphore available) {
        while (available.tryAcquire()) {
            try {
                processResult(completer.take());
            } catch (InterruptedException e) {
                return;
            }
        }
    }

    private void processResult(Future<T> result) throws InterruptedException {
        if (!result.isCancelled()) {
            try {
                reporter.setProcessorStatus(consumer.process(result.get()));
            } catch (Exception e) {
                reporter.addException();
            }
        }
    }

    protected final void cancelTasks(List<Future<T>> tasks) {
        reporter.setProducerStatus("Cancelling submitted tasks.");
        int cancelled = 0;
        for (Future<T> task : tasks) {
            if (task.cancel(false)) {
                cancelled++;
            }
        }
        reporter.setProducerStatus("Cancelled " + cancelled + " submitted tasks.");
    }
    
    @Override
    public final String getCurrentStatusMessage() {
        return reporter.toString();
    }
    
    private static class ProgressReporter<R extends Reducible<R>> {
        
        private String producerStatus;
        private UpdateProgress submissions;
        
        private int exceptions;
        private R processorStatus;
        
        public ProgressReporter() {
            reset();
        }
        
        public void reset() {
            producerStatus = null;
            submissions = UpdateProgress.START;
            processorStatus = null;
            exceptions = 0;
        }
        
        @Override
        public String toString() {
            if(producerStatus != null) {
                return producerStatus + 
                       submissions.toString(" %s tasks submitted" + (submissions.hasFailures() ? ", %s rejected. " : ". ")) + 
                       (processorStatus != null ? processorStatus.toString() : "") + 
                       (exceptions > 0 ? String.format(". %s exceptions", exceptions) : "");
            }
            return null;
        }
        
        public void addSubmission() {
            this.submissions = submissions.reduce(UpdateProgress.SUCCESS);
        }
        
        public void addRejection() {
            this.submissions = submissions.reduce(UpdateProgress.FAILURE);
        }
        
        public void setProducerStatus(String status) {
            this.producerStatus = status;
        }
        
        public void addException() {
            exceptions++;
        }
        
        public void setProcessorStatus(R o) {
            this.processorStatus = (processorStatus == null) ? o : processorStatus.reduce(o);
        }
        
    }
}

