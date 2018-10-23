// https://searchcode.com/api/result/61250656/

/*
 * Copyright 2011 Kevin A. Burton
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/
package peregrine.controller;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

import peregrine.*;
import peregrine.config.*;
import peregrine.controller.*;
import peregrine.http.*;
import peregrine.io.*;
import peregrine.io.driver.shuffle.*;
import peregrine.rpc.*;
import peregrine.task.*;

import com.spinn3r.log5j.*;

/**
 * 
 * Main interface for running a map reduce job in Peregrine.  The controller
 * communicates with worker nodes which will then in turn will run jobs with
 * your specified {@link Mapper}, {@link Merger} and/or {@link Reducer}.
 * 
 */
public class Controller {

    private static final Logger log = Logger.getLogger();

    private Config config = null;

    private ControllerDaemon daemon = null;
    
    protected ClusterState clusterState;

    /**
     * True if we are shutdown to avoid multiple shutdown attempts.
     */
    protected boolean shutdown = false;
    
    public Controller( Config config ) {
    	
        this.config = config;

        // verify that we aren't running on the right host.  Starting up a
        // controller on the wrong machine doesn't make sense and the job won't
        // work.
        
        String hostname = config.getHost().getName();
        String controllername = config.getController().getName();

        if ( ! hostname.equals( controllername ) ) {
            throw new RuntimeException( String.format( "Starting controller on incorrect host( %s vs %s )",
                                                       hostname, controllername ) );
        }

        // make sure to set the host to the controller so we use the right port.
        config.setHost( config.getController() );

        log.info( "Starting controller: %s", config.getController() );

        this.clusterState = new ClusterState( config, this );

        this.daemon = new ControllerDaemon( this, config, clusterState );

    }

    public void map( Class mapper,
                     String... paths ) throws Exception {
        map( mapper, new Input( paths ) );
    }

    public void map( final Class mapper,
                     final Input input ) throws Exception {
        map( mapper, input, null );
    }
    
    public void map( final Class delegate,
    		 		 final Input input,
    				 final Output output ) throws Exception {

    	map( new Job().setDelegate( delegate ) 
    			      .setInput( input )
    			      .setOutput( output ) );
    		
    }
    
    /**
     * Run map jobs on all chunks on the given path.
     */
    public void map( final Job job ) throws Exception {
    	
    	withScheduler( job, new Scheduler( "map", job, config, clusterState ) {

    			@Override
                public void invoke( Host host, Work work ) throws Exception {

                    Message message 
                        = createSchedulerMessage( "exec", job, work );

                    new Client().invoke( host, "map", message );
                    
                }
                
            } );

    }
    
    public void merge( Class mapper,
                       String... paths ) throws Exception {

        merge( mapper, new Input( paths ) );

    }

    public  void merge( final Class mapper,
                        final Input input ) throws Exception {

        merge( mapper, input, null );

    }

    public void merge( final Class delegate,
                       final Input input,
                       final Output output ) throws Exception {
    	
    	merge( new Job().setDelegate( delegate )
    			        .setInput( input )
    			        .setOutput( output ) );
        	
    }
    
    /**
     * 
     * Conceptually, <a href='http://en.wikipedia.org/wiki/Join_(SQL)#Full_outer_join'>
     * a full outer join</a> combines the effect of applying both left
     * and right outer joins. Where records in the FULL OUTER JOINed tables do not
     * match, the result set will have NULL values for every column of the table
     * that lacks a matching row. For those records that do match, a single row
     * will be produced in the result set (containing fields populated from both
     * tables)
     */
    public void merge( final Job job ) throws Exception {
    	
        withScheduler( job, new Scheduler( "merge", job, config, clusterState ) {

                @Override
                public void invoke( Host host, Work work ) throws Exception {

                    Message message = createSchedulerMessage( "exec", job, work );
                    new Client().invoke( host, "merge", message );
                    
                }
                
            } );
            
    }
  
    public void reduce( final Class delegate,
    				    final Input input,
    				    final Output output ) 
            		throws Exception {
    	
    	reduce( new Job().setDelegate( delegate )
		                 .setInput( input )
		                 .setOutput( output ) );    	
    }

    /**
     * Perform a reduce over the previous shuffle data (or broadcast data).
     */
    public void reduce( final Job job ) 
        throws Exception {

    	final Input input = job.getInput();
    	
        // we need to support reading input from the shuffler.  If the user
        // doesn't specify input, use the default shuffler.

        if ( input == null )
            throw new Exception( "Input may not be null" );
        
        if ( input.getReferences().size() < 1 ) {
            throw new IOException( "Reducer requires at least one shuffle input." );
        }

        // this will block for completion ... 
        withScheduler( job, new Scheduler( "reduce", job, config, clusterState ) {

                @Override
                public void invoke( Host host, Work work ) throws Exception {

                    Message message = createSchedulerMessage( "exec", job, work );
                    new Client().invoke( host, "reduce", message );
                    
                }
                
            } );

        for( InputReference ref : input.getReferences() ) {

            if ( ref instanceof ShuffleInputReference ) {

                ShuffleInputReference shuffle = (ShuffleInputReference)ref;

                log.info( "Going to purge %s for job %s", shuffle.getName(), job );
                
                purgeShuffleData( shuffle.getName() );

            }
            
        }

    }

    private void withScheduler( Job job, Scheduler scheduler ) 
    		throws Exception {

        String operation = scheduler.getOperation();
        
        String desc = String.format( "%s %s (%s) for input %s and output %s ",
                                     operation, job.getDelegate().getName(),
                                     job.getName(), job.getInput(), job.getOutput() );

        log.info( "STARTING %s", desc );

        long before = System.currentTimeMillis();
        
        daemon.setScheduler( scheduler );

        scheduler.waitForCompletion();

        daemon.setScheduler( null );

        // shufflers can be flushed after any stage even reduce as nothing will
        // happen

        // TODO: this should be a typesafe enum with map|merge|reducem
        
        if ( "map".equals( operation ) || "merge".equals( operation ) )
            flushAllShufflers();

        // now reset the worker nodes between jobs.
        reset();
        
        long after = System.currentTimeMillis();

        long duration = after - before;
        
        log.info( "COMPLETED %s (duration %,d ms)", desc, duration );

    }
    
    // TODO this probably should not be public.
    public void flushAllShufflers() throws Exception {

        Message message = new Message();
        message.put( "action", "flush" );

        callMethodOnCluster( "shuffler", message );
        
    }

    /**
     * Purge the shuffle data on disk so that it does not conflict with other jobs.
     * 
     * @throws Exception
     */
    public void purgeShuffleData( String name ) throws Exception {

        Message message = new Message();
        message.put( "action", "purge" );
        message.put( "name",   name );

        callMethodOnCluster( "shuffler", message );
        
    }

    /**
     * Reset cluster job state between jobs.
     */
    private void reset() throws Exception {

        Message message = new Message();
        message.put( "action", "reset" );

        callMethodOnCluster( "map",    message );
        callMethodOnCluster( "merge",  message );
        callMethodOnCluster( "reduce", message );
        
    }
    
    private void callMethodOnCluster( String service, Message message ) throws Exception {

        String desc = String.format( "CALL %s %,d hosts with message: %s" , message, config.getHosts().size(), message );
        
        log.info( "STARTING %s", desc );

        long before = System.currentTimeMillis();
        
        List<HttpClient> clients = new ArrayList();
        
        for ( Host host : config.getHosts() ) {
            clients.add( new Client().invokeAsync( host, service, message ) );
        }

        for( HttpClient client : clients ) {
            client.shutdown();
        }

        for( HttpClient client : clients ) {
            client.close();
        }

        long after = System.currentTimeMillis();

        long duration = after - before;

        log.info( "COMPLETED %s (duration %,d ms)", desc, duration );

    }
    
    private Message createSchedulerMessage( String action,
                                            Job job,
                                            Work work ) {

    	Class delegate = job.getDelegate();
    	Input input = job.getInput();
    	Output output = job.getOutput();
    
        Message message = new Message();
        
        message.put( "action",     action );
        message.put( "delegate",   delegate.getName() );
        message.put( "job_id",     job.getId() );
    	message.put( "work" ,      work.getReferences() );

        if ( input != null )
        	message.put( "input" ,  input.getReferences() );

        if ( output != null )
        	message.put( "output" , output.getReferences() );

        return message;
        
    }

    public void shutdown() {

        if ( shutdown )
            return;

        daemon.shutdown();

        shutdown = true;
        
    }
    
}

interface CallableFactory {

    public Callable newCallable( Partition part, Host host );

}


