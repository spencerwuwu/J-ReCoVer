// https://searchcode.com/api/result/125476570/

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
package peregrine;

import com.spinn3r.log5j.Logger;
import peregrine.controller.BaseJob;
import peregrine.controller.JobOperation;
import peregrine.controller.JobState;
import peregrine.io.Input;
import peregrine.io.Output;
import peregrine.rpc.Message;
import peregrine.sort.ComputePartitionTableJob;
import peregrine.sort.GlobalSortJob;
import peregrine.sort.GlobalSortPartitioner;
import peregrine.util.Getopt;
import peregrine.util.ObjectFormatter;
import peregrine.util.Strings;

import java.util.ArrayList;
import java.util.List;

/**
 * A 'batch' of jobs sent to the controller at once.
 *
 */
public class Batch extends BaseJob<Batch> {

    private static final Logger log = Logger.getLogger();

    protected List<Job> jobs = new ArrayList<Job>();

    protected int start = 0;

    protected int end = Integer.MAX_VALUE;

    /**
     * Needed for loading batches via RPC.
     */
    public Batch() {
        init( this );
    }

    public Batch( Class clazz ) {
        this( clazz.getName() );
    }

    public Batch( Job job ) {
        this( job.getName() );
    }

    public Batch( String name ) {
        this.name = name;
        init( this );
    }
    
    public Batch map( Class mapper,
                      String... paths ) {
        return map( mapper, new Input( paths ) );
    }

    public Batch map( Class mapper,
                      Input input ) {
        return map( mapper, input, null );
    }
    
    public Batch map( Class delegate,
                      Input input,
                      Output output ) {

    	return map( new Job().setDelegate( delegate ) 
                    .setInput( input )
                    .setOutput( output ) );
    		
    }

    public Batch map( Job job ) {

        useDefaultName( job );

        add( job.setOperation( JobOperation.MAP ) );

        return this;
    }

    public Batch merge( Class mapper,
                       String... paths ) {

        return merge( mapper, new Input( paths ) );
        
    }

    public Batch merge( Class mapper,
                        Input input ) {

        return merge( mapper, input, null );

    }

    public Batch merge( Class delegate,
                        Input input,
                        Output output ) {
    	
    	return merge( new Job().setDelegate( delegate )
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
    public Batch merge( Job job ) {

        useDefaultName( job );

        add( job.setOperation( JobOperation.MERGE ) );

        return this;
    }

    public Batch reduce( final Class delegate,
                         final Input input,
                         final Output output )  {

        return reduce( new Job().setDelegate( delegate )
                                .setInput( input )
                                .setOutput( output ) );

    }

    public Batch reduce( Job job ) {

        useDefaultName( job );

        add( job.setOperation( JobOperation.REDUCE ) );

        return this;
    }

    /**
     * Truncate / startup a file so that it is empty and ready to be merged against.
     */
    public Batch truncate( String path ) {

        // map-only job that reads from an empty blackhole: stream and writes
        // nothing to the output file. 
        
        return map( new Job().setDelegate( Mapper.class )
                             .setInput( "blackhole:" )
                             .setOutput( path )
                             .setDescription( "Truncate: " + path ) );
        
    }

    public Batch sort( String input, String output, Class comparator ) {

        map( new Job().setDelegate( ComputePartitionTableJob.Map.class )
                      .setInput( input )
                      .setOutput( "broadcast:partition_table" )
                      // this is only a sample job so we only need to read 1 chunk
                      .setMaxChunks( 1 )
                      // note that we can't use the normal job.setComparator
                      // here because this would mean we would use a custom
                      // partition layout when doing an emit and that is exactly
                      // what we want to avoid during the reduce.
                      .setParameters( "sortComparator", comparator.getName() ) )
                      .setDescription( String.format( "Sort %s", input ) );
             
        // Step 2.  Reduce that partition table and broadcast it so everyone
        // has the same values.
        reduce( new Job().setDelegate( ComputePartitionTableJob.Reduce.class )
                         .setInput( "shuffle:partition_table" )
                         .setOutput( "/tmp/partition_table" )
                         .setParameters( "sortComparator", comparator.getName() ) );

        // Step 3.  Map across all the data in the input file and send it to
        // right partition which would need to hold this value.
        
        Job job = new Job();

        job.setDelegate( GlobalSortJob.Map.class );
        job.setInput( new Input( input, "broadcast:/tmp/partition_table" ) );
        job.setOutput( new Output( "shuffle:default" ) );
        job.setPartitioner( GlobalSortPartitioner.class );
        job.setComparator( comparator );
        
        map( job );

        job = new Job();
        
        job.setDelegate( GlobalSortJob.Reduce.class );
        job.setInput( new Input( "shuffle:default" ) );
        job.setOutput( new Output( output ) );
        job.setComparator( comparator );
        
        reduce( job );

        return this;

    }

    private void useDefaultName( Job job ) {

        if ( Strings.empty( job.getName() ) )
            job.setName( job.getDelegate().getName() );

    }
    
    // **** basic / primitive operations
    
    public void add( Job job ) {
        jobs.add( job );
    }

    /**
     * Add all jobs in the given batches to the current batch.
     */
    public void add( Batch... batches ) {

        for( Batch batch : batches ) {

            if ( batch.getJobs().size() == 0 ) {
                throw new IllegalArgumentException( "Batch has no jobs: " + batch );
            }
            
            for ( Job job : batch.getJobs() ) {
                add( job );
            }

        }
        
    }

    public List<Job> getJobs() {
        return jobs;
    }

    public String getName() { 
        return this.name;
    }

    public Batch setName( String name ) { 
        this.name = name;
        return this;
    }

    public String getDescription() { 
        return this.description;
    }

    public Batch setDescription( String description ) { 
        this.description = description;
        return this;
    }

    public int getStart() {
        return this.start;
    }

    public Batch setStart( int start ) {
        this.start = start;
        return this;
    }

    public int getEnd() {
        return this.end;
    }

    public Batch setEnd( int end ) {
        this.end = end;
        return this;
    }

    public int getProgress() {

        int nr_jobs = getJobs().size();
        int nr_complete = 0;
        
        for( Job job : getJobs() ) {
            
            if ( job.getState().equals( JobState.COMPLETED ) )
                ++nr_complete;
            
        }

        return (int)(100 * (nr_complete / (double)nr_jobs));

    }
    
    public Job getExecutingJob() {

        for ( Job job : getJobs() ) {

            if ( job.getState().equals( JobState.EXECUTING ) )
                return job;

        }

        return null;
        
    }

    public String explain() {

        ObjectFormatter formatter = new ObjectFormatter( this, BaseJob.class, Batch.class );
        formatter.addIgnoredFieldByName( "instance" );
        return formatter.toString();

    }

    /**
     * Make sure this batch job is viable for execution.  It should have at
     * least 1 job, it should have a valid name.
     */
    public void assertExecutionViability() {

        if ( getJobs().size() == 0 ) {
            throw new RuntimeException( "Batch has no jobs" );
        }

        if ( Strings.empty( name ) ) {
            throw new RuntimeException( "Batch has no name" );
        }

    }

    /**
     * Command line apps should parse args from the command line.
     */
    public void init( String[] args ) {
        Getopt getopt = new Getopt( args );

        this.start = getopt.getInt( "batch.start", 0 );
        this.end   = getopt.getInt( "batch.end",   Integer.MAX_VALUE );
    }
    
    /**
     * Convert this to an RPC message.
     */
    @Override
    public Message toMessage() {

        Message message = super.toMessage();

        message.put( "start",         start );
        message.put( "end",           end );
        message.put( "jobs",          jobs );

        return message;
        
    }

    @Override
    public void fromMessage( Message message ) {

        super.fromMessage( message );
        
        start         = message.getInt( "start" );
        end           = message.getInt( "end" );
        jobs          = message.getList( "jobs", Job.class );

    }

    @Override
    public String toString() {
        return String.format( "%s jobs=%s", getName(), jobs.toString() );
    }
    
}

