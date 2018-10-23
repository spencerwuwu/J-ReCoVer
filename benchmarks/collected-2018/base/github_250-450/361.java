// https://searchcode.com/api/result/101202664/

package uk.ac.susx.tag.method51.core.agents;

/*
 * #%L
 * AbstractServiceAgent.java - method51 - University of Sussex - 2,013
 * %%
 * Copyright (C) 2013 - 2014 University of Sussex
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.Service;
import org.joda.time.Duration;
import org.joda.time.Interval;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.susx.tag.method51.core.params.codec.concrete.DurationCodec;
import uk.ac.susx.tag.method51.core.params.codec.concrete.TimestampCodec;
import uk.ac.susx.tag.method51.core.params.type.AType;

import javax.annotation.Nonnull;
import javax.annotation.OverridingMethodsMustInvokeSuper;
import java.util.concurrent.TimeUnit;

/**
 * {@code AbstractServiceAgent} is an {@link Agent} where the all the heavy lifting is delegated to a guava
 * {@link Service}. The execution strategy can be configured at run time.
 *
 * @author Hamish I A Morgan &lt;hamish.morgan@sussex.ac.uk&gt;
 * @since 24/10/2013
 */
public abstract class AbstractServiceAgent extends AbstractAgent {

    /**
     * How long (in millis) to block on polling the input queue. Larger values will reduce the
     * iteration overhead, but also reduce liveness.
     */
    protected static final long BLOCK_TIMEOUT = 10L;
    protected static final TimeUnit BLOCK_UNIT = TimeUnit.MILLISECONDS;
    private static final Logger LOG = LoggerFactory.getLogger(AbstractServiceAgent.class);
    @Nonnull
    protected final ExecutionStrategy.AgentWrapperService service;
    /**
     * Forking constructor.
     *
     * @param other instance to fork
     */
    protected AbstractServiceAgent(@Nonnull final AbstractServiceAgent other) {
        super(other);
        service = other.service.clone();
        initialiseService();
    }

    protected AbstractServiceAgent(@Nonnull final Options options) {
        super(options);

        ExecutionStrategy executionStrategy = options.executionStrategy.get();
        service = executionStrategy.createService(options);
        initialiseService();
    }

    private void initialiseService() {
        service.setAgent(this);
        service.addListener(new Service.Listener() {

            @Override
            public void starting() {
                setState(State.STARTING);
            }

            @Override
            public void running() {
                setState(State.RUNNING);
            }

            @Override
            public void stopping(Service.State from) {
                setState(State.STOPPING);
            }

            @Override
            public void terminated(Service.State from) {
                setState(State.TERMINATED);
            }

            @Override
            public void failed(Service.State from, Throwable failure) {
                setStateFailure(failure);
            }
        }, MoreExecutors.sameThreadExecutor());
    }

    protected void requireChronologicalService() {
        if(!(service instanceof ExecutionStrategy.ChronologicalAgentWrapperService) ) {
            throw new IncorrectServiceTypeException(ExecutionStrategy.ChronologicalAgentWrapperService.class.getName() + " required");
        }
    }

    protected ExecutionStrategy.ChronologicalAgentWrapperService getChronologicalService() {
        requireChronologicalService();
        return (ExecutionStrategy.ChronologicalAgentWrapperService) service;
    }

    // ============================================================================================

    // ============================================================================================
    // Execution methods to be implemented/overridden
    // ============================================================================================


    //private long _tick = 0;

    protected final void run() throws Exception {
        // Keep iterating until shutdown
        while (isRunning()) {
            if (isPaused()) {
                awaitResumed(BLOCK_TIMEOUT, BLOCK_UNIT);
            } else {
                //LOG.info("{} {}", this.getClass().getName(), _tick++);
                runOneIteration();
                Thread.yield();
            }
        }
    }

    /**
     * Subclasses must implement this method with whatever code is required on each iteration of the agent. Iterations
     * are run based on the executed strategy. 
     * <p/>
     * The method is guaranteed to run only after {@link #startUp()} has been called. If an exception is thrown by this 
     * method, the agent will transition to {@link State#FAILED}. To terminate the agent this method may invoke
     * {@link #stopAsync()}, which will cause the agent to stop eventually. whether the agent terminates or fails, the
     * {@link #shutDown()} is guaranteed to run after the last execution of {@code runOneIteration}.
     * 
     * @throws Exception
     */
    protected abstract void runOneIteration() throws Exception;

    /**
     * Subclasses can optionally override this method with whatever initialization code is required.
     * <p/>
     * The method is guaranteed to run (and complete) before {@link #runOneIteration()} and {@link #shutDown()}. If an
     * exception is thrown by this method, the agent will immediately transition to {@link State#FAILED}; it will never
     * run, not will {@link #shutDown()} be called.
     * <p/>
     * <strong>Overriding methods <em>must</em> call {code super.startUp()} first (i.e before they do anything).
     * </strong> 
     * 
     * @throws Exception
     */
    @OverridingMethodsMustInvokeSuper
    protected void startUp() throws Exception {
    }

    /**
     * Subclasses can optionally override this method with whatever clean-up code is required.
     * <p/>
     * The method is run before the agent enters {@link State#TERMINATED} or {@link State#FAILED}. It is guaranteed to 
     * run after {@link #startUp()} and {@link #runOneIteration()}. If an exception is thrown by this method, the
     * agent will immediately transition to {@link State#FAILED}. 
     * <p/>
     * Note that this method will not be run if an exception is thrown during {@link #startUp()}, because it is assumed
     * that initialization failed, so cleanup is not required. It will, however, be run if an exception is thrown in 
     * the run loop. If you want to absolutely guarantee that something is run when the agent stops, you need to add
     * a listener to the agent with the appropriate action.
     * <p/>
     * <strong>Overriding methods <em>must</em> call {code super.shutDown()}, probably as the last statement.
     * 
     * @throws Exception
     */
    @OverridingMethodsMustInvokeSuper
    protected void shutDown() throws Exception {
    }

    // ============================================================================================
    // Lifecycle methods delegated to the service
    // ============================================================================================

    @Override
    public final void startAsync() {
        assert getState() == State.NEW;
        service.startAsync();
    }

    @Override
    public final void stopAsync() {
        service.stopAsync();
    }

    // ============================================================================================

    public static class Options extends AbstractAgent.Options {

        private static final long DEFAULT_SCHEDULE_DELAY = 1000L;
        private static final double DEFAULT_SPEED = 1.0;


        protected final Param<Double> speed = new Param<>();
        {
            speed.doc("speed factor for chronological producers");
            speed.defaultValue(DEFAULT_SPEED);
        }
        protected final Param<Long> start = new Param<>(new AType<>(new TimestampCodec()));
        {
            start.doc("unix time stamp of the begin date");
            start.defaultValue(0L);
        }
        protected final Param<Long> end = new Param<>(new AType<>(new TimestampCodec()));
        {
            end.doc("unix time stamp of the end date");
            end.defaultValue(Long.MAX_VALUE);

        }

        protected final Param<ExecutionStrategy> executionStrategy = new Param<>();
        {
            executionStrategy.defaultValue(ExecutionStrategy.CONTINUOUS);
            executionStrategy.doc("How the run loop of this agent is executed;" +
                    " continuously, at a fixed rate, or not at all.");
        }

        protected final Param<Duration> interval = new Param<>(new AType<>(new DurationCodec()));
        {
            interval.doc("timing interval");
            interval.defaultValue(new Duration(1000L));
        }

        public void setSchedule(long scheduleDelay, TimeUnit scheduleUnit) {

            interval.set(new Duration(TimeUnit.MILLISECONDS.convert(scheduleDelay, scheduleUnit)));
        }

        public final void setExecutionStrategy(ExecutionStrategy executionStrategy) {
            this.executionStrategy.set(executionStrategy);
        }

        public final void setDefaultExecutionStrategy(ExecutionStrategy executionStrategy) {
            this.executionStrategy.defaultValue(executionStrategy);
        }

        public ExecutionStrategy getExecutionStrategy() {
            return executionStrategy.get();
        }

        public long getInterval() {
            return interval.get().getMillis();
        }

        public long getInterval(TimeUnit unit) {
            return unit.convert(interval.get().getMillis(), TimeUnit.MILLISECONDS);
        }

        public void setInterval(long duration) {
            interval.set(new Duration(duration));
        }

        public void setInterval(long duration, TimeUnit unit) {
            interval.set(new Duration(TimeUnit.MILLISECONDS.convert(duration, unit)));
        }

        public double getSpeed() {
            return speed.get();
        }

        public void setSpeed(double speed) {
            this.speed.set(speed);
        }

        public long getStart(TimeUnit unit) {
            return unit.convert(start.get(), TimeUnit.MILLISECONDS);
        }

        public long getStart() {
            return start.get();
        }

        public void setStart(long start, TimeUnit unit) {
            this.start.set(TimeUnit.MILLISECONDS.convert(start, unit));
        }

        public void setStart(long start) {
            this.start.set(start);
        }

        public long getEnd(TimeUnit unit) {
            return unit.convert(end.get(), TimeUnit.MILLISECONDS);
        }

        public long getEnd() {
            return end.get();
        }

        public void setEnd(long end, TimeUnit unit) {
            this.end.set(TimeUnit.MILLISECONDS.convert(end, unit));
        }

        public void setEnd(long end) {
            this.end.set(end);
        }

        public Interval getLifetime() {
            return new Interval(getStart(TimeUnit.MILLISECONDS), getEnd(TimeUnit.MILLISECONDS));
        }

    }

}

