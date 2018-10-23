// https://searchcode.com/api/result/12288712/

package com.asamioffice.goldenport.appengine;

import static com.google.appengine.api.labs.taskqueue.TaskOptions.Builder.url;

import java.util.Date;
import java.util.logging.Logger;

import com.google.appengine.api.labs.taskqueue.Queue;
import com.google.appengine.api.labs.taskqueue.QueueFactory;
import com.google.appengine.api.labs.taskqueue.TaskOptions;

/**
 * @since   Oct.  2, 2010
 * @version Oct.  2, 2010
 * @author  ASAMI, Tomoharu
 */
public abstract class AppengineServiceBase {
    static final Logger logger = Logger.getLogger(EntityTools.class.getName());

    protected long retry_Countdown_Millis() {
        return 3000;
    }

    protected int retry_Count() {
        return 10;
    }

    protected boolean use_Task_Reschedule() {
        return false;
    }

    protected abstract String get_Task_Queue_Url();

    protected void execute_task(Task task) {
        try {
            if (task.isFirst()) {
                Date startDateTime = new Date();
                logger.finer("START[" + startDateTime + "]" + task.getInfo());
                task.run();
                Date endDateTime = new Date();
                long ellapse = endDateTime.getTime() - startDateTime.getTime();
                logger.finer("END[" + ellapse + "]");
            } else {
                logger.finer("IGNORE: " + task.getInfo());
            }
        } catch (RuntimeException e) {
            if (use_Task_Reschedule()) {
                logger.warning("RETRY[" + get_exception_message(e) + "] " + task.getInfo());
                add_task_queue_retry(task);
            } else {
                logger.severe("ERROR[" + get_exception_message(e) + "] " + task.getInfo());
                throw e;
            }
            return;
        }
        Task[] tasks = task.getNextTasks();
        if (tasks != null) {
            for (Task next: tasks) {
                if (next.isFirst()) {
                    add_task_queue(next);
                } else {
                    logger.finer("IGNORE: " + next.getInfo());
                }
            }
        }
    }

    protected final void schedule_task(Task task) {
        add_task_queue(task);        
    }

    private void add_task_queue(Task task) {
        for (int i = 0; i < retry_Count(); i++) {
            try {
                add_task_queue_core(task);
                return;
            } catch (RuntimeException e) {
                int next = i + 1;
                if (next >= retry_Count()) {
                    throw e;
                } else {
                    logger.warning("Retry[" + get_exception_message(e) + "] count=" + next);
                }
            }
        }
        throw new InternalError("not reached");
    }

    private void add_task_queue_core(Task task) {
        Queue queue = QueueFactory.getDefaultQueue();
        String taskUrl = task.getUrl(get_Task_Queue_Url());
        TaskOptions req = task.buildRequest(url(taskUrl));
        queue.add(req);
        logger.fine("ADD TASK:" + task.getInfo() + ", options = " + req.getUrl());
    }

    private void add_task_queue_retry(Task task) {
        for (int i = 0; i < retry_Count(); i++) {
            try {
                add_task_queue_retry_core(task);
                return;
            } catch (RuntimeException e) {
                int next = i + 1;
                if (next >= retry_Count()) {
                    throw e;
                } else {
                    logger.warning("Retry[" + get_exception_message(e) + "] count=" + next);
                }
            }
        }
        throw new InternalError("not reached");
    }

    private void add_task_queue_retry_core(Task task) {
        Queue queue = QueueFactory.getDefaultQueue();
        logger.finer(task.getInfo());
        String taskUrl = task.getUrl(get_Task_Queue_Url());
        TaskOptions req = task.buildRequest(url(taskUrl)
            .countdownMillis(retry_Countdown_Millis()));
        queue.add(req);
        logger.fine("ADD TASK:" + task.getInfo() + ", options = " + req.getUrl());
    }

    protected final String get_exception_message(Exception e) {
        return e.getClass().getName() + ":" + e.getMessage();
    }

    protected static abstract class Task implements Runnable {
        public abstract String getInfo();

        /**
         * Checks whether the action is the first time or not. It's for to
         * reduce redundant actions by TQ's multiple task scheduling behavior.
         * This feature does not ensure the idempotent property of the action.
         * 
         * @return whether task should be execute or not.
         */
        public abstract boolean isFirst();

        public String getUrl(String url) {
            return url;
        }

        public TaskOptions buildRequest(TaskOptions options) {
            return options;
        }

        public Task[] getNextTasks() {
            return new Task[0];
        }
    }
}

