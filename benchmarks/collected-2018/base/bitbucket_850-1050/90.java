// https://searchcode.com/api/result/122323439/

/*
 * Copyright 2011-2013 Kevin A. Burton
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package peregrine.task;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.spinn3r.log5j.Logger;
import peregrine.Job;
import peregrine.config.Config;
import peregrine.config.Host;
import peregrine.config.Membership;
import peregrine.config.Partition;
import peregrine.controller.ClusterState;
import peregrine.io.InputReference;
import peregrine.io.driver.IODriver;
import peregrine.io.driver.IODriverRegistry;
import peregrine.rpc.Message;
import peregrine.rpc.RpcClient;
import peregrine.util.*;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * Handles work scheduling in the cluster.
 * 
 * <h2>Overview</h2>
 * 
 * <p>
 * Handles work scheduling in the cluster.  RPC endpoints deliver messages
 * to the scheduler, periodically it wakes up and looks for empty slots on
 * machines which can do work and then schedules jobs when necessary.
 *
 * <h2>Speculative Execution</h2>
 * 
 * <p> Speculative execution works by first executing partitions that have a
 * higher priority.  Once we have spare hosts which have completed all their
 * primary work, we do speculative execution on them by running partition which
 * may be in flight on other hosts.
 *
 * <p> The system works by first sorting all potential replicas on the host by
 * looking at the number of currently executing jobs and sorting them ascending
 * so that jobs which have LESS speculative executions get bumped up higher.
 *
 * <p> Once one of the hosts reports a partition as complete, we mark it
 * complete internally and then add requests into a prey queue to send RPC
 * messages to these hosts to kill the extra tasks.
 *
 * <p> When the tasks die they send off an RPC message saying that they were
 * failed and marked killed at which make these hosts available for more work.
 * 
 * <h2> Speculative Sorting</h2>
 *
 * <p>When shuffle data is sent from map tasks, and there is a shuffle target,
 * we can preemptively sort these files when machines are idle. They have to be
 * sorted ANYWAY and we might as well do this while we have idle CPU time.
 */
public class Scheduler {

    private static final Logger log = Logger.getLogger();

    protected Config config = null;

    protected Membership membership = null;

    /**
     * The list of partitions that are completed.  When a new MapperTask needs
     * more work we verify that we aren't scheduling work form completed
     * partitions.
     */
    protected MarkSet<Work> completed = new MarkSet<Work>();

    /**
     * The list of work that has been scheduled for execution but not yet been
     * completed.
     */
    protected MarkSet<Work> scheduled = new MarkSet<Work>();

    /**
     * Keep track of which hosts are performing jobs on which work units.  This
     * is used so that we can enable speculative execution as we need to
     * terminate work hosts which have active work but another host already
     * completed it.
     */
    protected MapSet<Work,Host> executing = new MapSet<Work,Host>();
    
    /**
     * Hosts which are available for additional work.  These are stored in a 
     * queue so we can just keep popping items until it is empty. 
     */
    protected SimpleBlockingQueue<Host> available = new SimpleBlockingQueue<Host>();

    /**
     * Hosts available for additional work for speculative execution.
     */
    protected MarkSet<Host> spare = new MarkSet<Host>();

    /**
     * The concurrency of each host as it is executing.
     */
    protected IncrMap<Host> concurrency;

    /**
     * Failure conditions in the scheduler so that we can fail easily.
     */
    protected MarkSet<Fail> failure = new MarkSet<Fail>();

    /**
     * When a host has has been marked offline, we also need to keep track of
     * the partitions it hosted.  When number of offline hosts for a partition
     * is equal to the replica count the system has to fail as we have lost
     * data (ouch).
     */
    protected IncrMap<Work> offlineWork;

    /**
     * Hosts which should be sent kill requests because a job completed which is
     * also being speculatively executed on other hosts so we need to send a
     * kill command to the host.
     */
    protected SimpleBlockingQueue<Work> prey = new SimpleBlockingQueue<Work>();

    /**
     * Keep track of the work to perform by host.  On scheduler startup we first
     * call getWork() on all the input and then build the work index from this
     * data structure.
     */
    protected Map<Host,List<Work>> workIndex = new ConcurrentHashMap<Host,List<Work>>();

    /**
     * All known work (sorted) for this job.  This is immutable and won't change
     * at runtime as a full reference of all the work needed.
     */
    protected SortedSet<Work> work = new TreeSet<Work>();

    /**
     * Keep track of reporters send within progress messages so that we can
     * return this state information status client.
     */
    protected Map<Partition,Report> reports = new ConcurrentHashMap<Partition,Report>();
    
    protected ChangedMessage changedMessage = new ChangedMessage();

    private ClusterState clusterState;

    private Job job;

    private String operation;

    protected Scheduler() {}; /* for testing */
    
    public Scheduler( final String operation,
                      final Job job,
                      final Config config,
                      final ClusterState clusterState )
        throws IOException {

        log.info( "Creating new scheduler for %s on job: %s" , operation, job );
        
        this.operation = operation;
        this.job = job;
        this.config = config;
        this.membership = config.getMembership();
        this.clusterState = clusterState;

        // create a concurrency from all the currently known hosts.
        concurrency = new IncrMap<Host>( config.getHosts() );

        workIndex = createWorkIndex();

        offlineWork = new IncrMap<Work>();
        work = new TreeSet<Work>( new Comparator<Work>() {

                public int compare( Work w1, Work w2 ) {
                    return w1.toString().compareTo( w2.toString() );
                }
                
            } );

        // make sure EVERY work unit has an entry in the offlineWork index so
        // that when we do a get() we can measure that it is zero (and not
        // null).

        for( Host host : workIndex.keySet() ) {

            List<Work> workForHost = workIndex.get( host );

            for( Work current : workForHost ) {
                offlineWork.init( current );
                work.add( current );
            }
            
        }
        
        // import the current list of online hosts and pay attention to new
        // updates in the future.
        
        clusterState.getOnline().addListenerWithSnapshot( new MarkListener<Host>() {

                public void updated( Host host, MarkListener.Status status ) {

                    if ( status == MarkListener.Status.MARKED ) {
                        log.info( "Host now available: %s", host );
                        available.putWhenMissing( host );
                    }
                    
                }

            } );

        clusterState.getOffline().addListenerWithSnapshot( new MarkListener<Host>() {

                public void updated( Host host, MarkListener.Status status ) {

                    // for every partition when it is marked offline, go through
                    // and mark every partition offline.  if a partition has NO
                    // online replicas then we must abort the job because there
                    // are no hosts with this data.

                    List<Work> workForHost = workIndex.get( host );

                    for( Work current : workForHost ) {
                        
                        offlineWork.incr( current );

                        if( offlineWork.get( current ) == config.getReplicas() ) {
                            markFailed( host, current , false, "*HOST MARKED OFFLINE*" );
                            break;
                        }

                    }

                }

            } );

    }

    /**
     * For each input reference, get a list of work from the driver, for a given
     * host.
     */
    protected Map<Host,List<Work>> createWorkIndex() throws IOException {
    
    	Map<Host,List<Work>> result = new ConcurrentHashMap();
    	
    	// create empty lists for every host... 
    	for( Host host : config.getHosts() ) {
    		result.put( host, new ArrayList() );
    	}

        // build the index of work now based on the IO driver ...
        
        for( InputReference inputReference : job.getInput().getReferences() ) {
        	
        	IODriver driver = IODriverRegistry.getInstance( inputReference.getScheme() );
               
        	Map<Host,List<Work>> driverWork = driver.getWork( config, inputReference );

        	for( Host host : driverWork.keySet() ) {
        		
        		List<Work> resultWorkEntry = result.get( host );
                List<Work> driverWorkEntry = driverWork.get( host );
        		
                for( int i = 0; i < driverWorkEntry.size(); ++i ) {
                	
                	Work work = null;
                	
                	if ( resultWorkEntry.size() - 1 >= i ) 
                        work = resultWorkEntry.get( i );
                	
                	if ( work == null ) {
                		work = driverWorkEntry.get( i );
                		resultWorkEntry.add( work );
                	} else {
                        // merge the two lists.. 
                		work.merge( driverWorkEntry.get( i ) );
                	}
                	
                }
                            
        	}
        	
        }	
        
        return result;
    	
    }
    
    /**
     * The operation in progress.  Can be map reduce or merge.
     */
    public String getOperation() {
        return operation;
    }

    public ImmutableSet<Work> getCompleted() {
        return completed.copyOf();
    }

    public ImmutableSet<Work> getScheduled() {
        return scheduled.copyOf();
    }

    public ImmutableMap<Work,Set<Host>> getExecuting() {
        return executing.copyOf();
    }

    public SimpleBlockingQueue<Host> getAvailable() {
        return available;
    }
    
    public Job getJob() {
        return job;
    }
    
    protected void schedule( Host host ) throws Exception {

        List<Work> workList = getWorkForExecutionByImportance( host );

        if ( workList.size() == 0 )
            log.warn( "NO work found for host: %s" , host );
        
        for( Work work : workList ) {
            
            if ( completed.contains( work ) ) {
                continue;
            }

            if ( config.getSpeculativeExecutionEnabled() ) {

                // verify that this host isn't ALREADY executing this partition
                // which would be wrong.
                if ( executing.contains( work ) && executing.get( work ).contains( host ) ) {
                    continue;
                }
                
            } else {

                if ( work.getPriority() > 0 ) {
                    continue;
                }
                
                if ( scheduled.contains( work ) ) {
                    // skip speculatively executing this partition now.
                    continue;
                }

            }

            // NOTE that this needs to be in the for loop for replica selection
            // because we want to keep filling this host with jobs until we
            // reach the desired concurrency.
            
            if ( concurrency.get( host ) >= config.getConcurrency() ) {
                return;
            }
            
            log.info( "Scheduling %s on %s with current concurrency: %,d of %,d",
                      work, host, concurrency.get( host ), config.getConcurrency() );
            
            invoke( host, work );

            // mark this host as scheduled so that work doesn't get executed again
            // until we want to do speculative execution

            scheduled.mark( work );

            concurrency.incr( host );

            executing.put( work, host );
            
            continue;

        }

        spare.mark( host );
        
    }

    /**
     * For a given host, return the replicas that it should process, ordered by
     * number of hosts currently running a job on that partition and then the
     * priority.
     */
    protected List<Work> getWorkForExecutionByImportance( Host host ) 
        throws Exception {

        List<Work> work = workIndex.get( host );

        if ( work == null )
            throw new Exception( "No work defined for host: " + host );

        return getWorkForExecutionByImportance( work );
        
    }

    /**
     * For a given host, return the replicas that it should process, ordered by
     * number of hosts currently running a job on that partition and then the
     * priority.
     */
    protected List<Work> getWorkForExecutionByImportance( List<Work> workForHost )
        throws Exception {

        final IncrMap<Work> parallelism = new IncrMap();

        final List<Work> result = new ArrayList();
                
        // add all these partitions to the mix.
        for( Work work : workForHost ) {
            
            parallelism.init( work );
            
            if ( executing.contains( work ) )
                parallelism.set( work, executing.get( work ).size() );
            
            result.add( work );
            
        }

        // now sort the result correctly.
        Collections.sort( result, new Comparator<Work>() {

                public int compare( Work w0, Work w1 ) {
                
                    int diff = parallelism.get( w0 ) - parallelism.get( w1 );

                    if ( diff != 0 )
                        return diff;

                    // now order it by priority
                    return w0.compareTo( w1 );

                }
                
            } );
        
        return result;
        
    }
    
    /**
     * Must be implemented by schedulers to hand out work correctly.
     */
    public void invoke( Host host, Work work ) throws Exception {

        // we could make this an abstract class but this means that we can't
        // test it as easily.

        throw new RuntimeException( "not implemented" );
        
    }

    /**
     * New report information is available.
     */
    public void updateReport( Report report ) {
        reports.put( report.getPartition(), report );
    }

    /**
     * Get a report representing the GLOBAL state of all reports acros all
     * partitions reporting.
     */
    public Report getReport() {

        Report result = new Report();

        for( Report current : reports.values() ) {
            result = result.plus( current );
        }

        return result;
        
    }
    
    /**
     * Mark a job as complete.  The RPC service calls this method when we are
     * List<Partition> partitions = config..getMembership()done with a job.
     */
    public void markComplete( Host host, Work work ) {

        if ( work.getReferences().size() == 0 )
            throw new RuntimeException( "Work is invalid" );
        
        log.info( "Marking work %s complete from host %s", work, host );
        
        // mark this partition as complete.
        completed.mark( work );

        markInactive( host, work );

        // for each one of the hosts that are executing.. Add them to the prey
        // queue so they can be killed.

        if ( executing.contains( work ) ) {
            
            for( Host current : executing.get( work ) ) {

                if ( current.equals( host ) )
                    continue;
                
                Work victim = work.copy();
                victim.setHost( current );
                
                prey.put( victim );

            }
            
        }

    }

    /**
     * Mark a job as failed.
     */
    public void markFailed( Host host,
                            Work work,
                            boolean killed,
                            String stacktrace ) {
    	
        log.error( "Host %s has failed on %s with trace: \n %s", host, work, stacktrace );

        markInactive( host, work );
        
        // this isn't really a failure because another host finished this job.
        if ( completed.contains( work ) )
            return;
        
        failure.mark( new Fail( host, work, stacktrace ) );
        
    }

    /**
     * Mark a given 
     */
    protected void markInactive( Host host, Work work ) {
    	
        // now remove this host from the list of actively executing jobs.
        executing.remove( work, host );

        // TODO: if this partition has other hosts running this job, terminate
        // the jobs.  This is needed for speculative execution.
        
        // clear the scheduled status for this partition. Note that we need to do
        // this AFTER marking it complete because if we don't then it may be
        // possible to read both completed and scheduled at the same time and both
        // would be clear.

        scheduled.clear( work );

        // add this to the list of available hosts so that we can schedule 
        // additional work.
        available.putWhenMissing( host );

        concurrency.decr( host );

    }
    
    public void markOnline( Host host ) {

    	if ( ! clusterState.getOnline().contains( host ) ) {    		
    		log.info( "Host is now online: %s", host );
    		available.putWhenMissing( host );
    	}

    }

    /**
     * Wait for all jobs to be complete.  This also performs scheduling in this
     * thread until we are done executing.
     */
    public void waitForCompletion() throws Exception {

        log.info( "Waiting on completion %s" , job );
        
        while( true ) {

            // test if we're complete.

            job.setReport( getReport() );
            
            if ( completed.size() == offlineWork.size() ) {
                break;
            }
            
            // Right now , if ANYTHING has failed, we can not continue.
            if ( failure.size() > 0 ) {

                StringBuilder buff = new StringBuilder();
                
                // log all root causes.
                for( Fail fail : failure.values() ) {

                    String message = String.format( "Failed to handle task: %s \n %s" , fail , fail.stacktrace );
                    
                    log.error( "%s" , message );

                    buff.append( message );
                    buff.append( "\n" );
                    
                }

                // throw the current position.
                throw new Exception( String.format( "Failed job %s due to %s \n%s\n", job, failure, buff.toString() ) );
                
            }

            // try to drain the prey queue killing each of the entries TODO. we
            // should probably use parallel dispatch on these when there are
            // multiple entries for performance reasons.
            while( prey.size() > 0 ) {

                Work victim = prey.take();
                sendKill( operation, victim.getHost(), victim );
                
            }

            // TODO: make this a constant.
            Host availableHost = available.poll( 500, TimeUnit.MILLISECONDS );

            if ( availableHost != null ) {

                log.debug( "Scheduling work for execution on: %s", availableHost );

                try {
                    schedule( availableHost );
                } catch ( Exception e ) {
                    //FIXME: we need to gossip about this.
                    log.error( "Unable to schedule work on host: " + availableHost, e );
                }

            }

            String status = status();
            
            if ( changedMessage.hasChanged( status ) ) {
                // all/most of this information is now present in status console app
                log.debug( "%s", status );
            }

            changedMessage.update( status );

        }
            
    }

    /**
     * Send a kill command to a given host to kill a job on a given partition.
     */
    protected void sendKill( String service,
                             Host host,
                             Work work ) {

        if ( host == null )
            throw new NullPointerException( "host" );
        
        while( true ) {

            try {

                if ( clusterState.getOffline().contains( host ) ) {
                    log.info( "Not sending kill.  Host is offline." );
                    return;
                }
                
                Message message = new Message();
                
                message.put( "action" , "kill" );
                message.put( "work", work.getReferences() );
                
                log.info( "Sending kill message to host %s: %s", host, message );
                
                new RpcClient( config ).invoke( host, service, message );
                
                break;

            } catch ( IOException e ) {
                
                log.error( String.format( "Unable to kill %s on %s for %s", service, host, work ), e );
                Threads.nap( 1500L );
                
            }
            
        } 
            
    }

    private String status() {

        Map<String,String> status = getStatusAsMap();
        
        StringBuilder buff = new StringBuilder();

        buff.append( String.format( "-- progress (%s) for %s %s: --\n",
                                    status.get( "perc" ),
                                    status.get( "operation" ),
                                    status.get( "job" ) ) );
        
        buff.append( String.format( "  progress:   %s\n" +
                                    "  available:  %s\n" +
                                    "  spare:      %s\n" +
                                    "  online:     %s\n" +
                                    "  failure:    %s\n", 
                                    status.get( "progress" ),
                                    status.get( "available" ),
                                    status.get( "spare" ),
                                    status.get( "online" ),
                                    status.get( "failure" ) ) );

        buff.append( String.format( "  %s\n", createLegend() ) );
        
        buff.setLength( buff.length() - 1 ); // trim the trailing \n
        
        return buff.toString();

    }

    /**
     * Get the current status as a map.
     */
    public Map<String,String> getStatusAsMap() {

        Map<String,String> result = new HashMap();

        result.put( "perc",         Integer.toString( getProgress() ) );
        result.put( "operation",    getOperation() );
        result.put( "job",          getJob().toString() );
                
        result.put( "progress",     createProgressBitMap() );
        result.put( "available",    getAvailable().toString() );
        result.put( "spare",        createBitMap( spare ) );
        result.put( "online",       createBitMap( clusterState.getOnline() ) );
        result.put( "failure",      failure.toString() );

        return result;
        
    }

    public int getProgress() {

        int progress = (int)(100 * (completed.size() / (double)offlineWork.size()));
        return progress;

    }
    
    /**
     * 
     */
    private String createBitMap( MarkCollection<Host> set ) {

        StringBuilder buff = new StringBuilder();

        for( Host current : config.getHosts() ) {

            if ( set.contains( current ) ) {
                buff.append( "*" );
            } else {
                buff.append( "." );
            }

        }

        return buff.toString();
        
    }
    
    /**
     * Use the existing work list to create a progress bit map.
     */
    private String createProgressBitMap() {

        StringBuilder buff = new StringBuilder();
        
        for( Work current : work ) {

            if ( scheduled.contains( current ) ) {
                buff.append( "S" );
                continue;
            }

            if ( completed.contains( current ) ) {
                buff.append( "C" );
                continue;
            }

            buff.append( "P" );

            //if ( buff.length() % 80 == 0 )
            //    buff.append( "    \n" );
            
        }

        return buff.toString();
        
    }

    private String createLegend() {

        return "Legend: S=scheduled, C=completed, P=pending";
        
    }
    
    private String format( MarkSet<Work> set ) {

        StringBuilder buff = new StringBuilder();

        buff.append( "[" );
        
        for( Work val : set.values() ) {

            if ( buff.length() > 1 )
                buff.append( ", " );

            buff.append( val.toString() );
            
        }

        buff.append( "]" );
        
        return buff.toString();
        
    }
    
}

class Fail implements Comparable<Fail> {

    protected Host host;
    protected Work work;

    protected String stacktrace;
    
    public Fail( Host host,
                 Work work,
                 String stacktrace ) {
        
        this.host = host;
        this.work = work;
        this.stacktrace = stacktrace;
        
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!( o instanceof Fail )) return false;

        Fail fail = (Fail) o;

        if (!host.equals( fail.host )) return false;
        if (!work.equals( fail.work )) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = host.hashCode();
        result = 31 * result + work.hashCode();
        return result;
    }

    @Override
    public int compareTo(Fail o) {
        int cmp = work.compareTo( o.work );

        if ( cmp != 0 )
            return cmp;

        return host.compareTo( o.host );

    }

    public String toString() {
        return String.format( "%s:%s", host, work );
    }
    
}

/**
 * A message which returns true if it is different from the previous message.
 */
class ChangedMessage {

    public String last = null;

    public boolean hasChanged( String message ) {
        return ! message.equals( last );
    }

    public void update( String message ) {
        last = message;
    }

}


