// https://searchcode.com/api/result/122323332/

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
import peregrine.config.partitioner.Partitioner;
import peregrine.config.partitioner.RangePartitioner;
import peregrine.controller.BaseJob;
import peregrine.controller.JobOperation;
import peregrine.io.Input;
import peregrine.io.Output;
import peregrine.rpc.Message;
import peregrine.sort.DefaultSortComparator;
import peregrine.sort.SortComparator;
import peregrine.task.Report;
import peregrine.util.NonceFactory;

import java.util.Map;

/**
 * Represents a job (map, merge, or, reduce) which much be run by Peregrine.
 * All necessary metadata is included here and specified for an entire job.
 *
 */
public class Job extends BaseJob<Job> {

    private static final Logger log = Logger.getLogger();

    protected long timestamp = System.currentTimeMillis();

	protected Class delegate = Mapper.class; 

	protected Class combiner = null;

	protected Input input = new Input();

	protected Output output = new Output();

    protected Class partitioner = RangePartitioner.class;

    protected Partitioner partitionerInstance = null;

    protected int maxChunks = Integer.MAX_VALUE;

    protected Class comparator = DefaultSortComparator.class; 

    protected Message parameters = new Message();

    protected String operation = JobOperation.MAP;

    protected Report report = new Report();

    public Job() {
        init( this );
        setIdentifier( NonceFactory.newNonce() );
        setName( "" + getIdentifier() );
    }

	public Class getDelegate() {
		return delegate;
	}
	public Job setDelegate(Class delegate) {
		this.delegate = delegate;
		return this;
	}
	public Class getCombiner() {
		return combiner;
	}
	public Job setCombiner(Class combiner) {
		this.combiner = combiner;
		return this;
	}
	public Input getInput() {
		return input;
	}

    public Job setInput( String... args ) {
        return setInput( new Input( args ) );
    }
    
	public Job setInput(Input input) {
		this.input = input;
		return this;
	}
	public Output getOutput() {
		return output;
	}

	public Job setOutput(String... args) {
        return setOutput( new Output( args ) );
    }
        
	public Job setOutput(Output output) {
		this.output = output;
		return this;
	}

    /**
     * The time the job was started on the controller.
     */
    public long getTimestamp() {
        return timestamp;
    }

    public Class getPartitioner() { 
        return this.partitioner;
    }

    public void setPartitioner( Class partitioner ) { 
        this.partitioner = partitioner;
    }

    public Partitioner getPartitionerInstance() {

        // we do not need the double check idiom here because this isn't
        // multithreaded code.
        
        if ( partitionerInstance == null ) {
        
            try {
                partitionerInstance = (Partitioner)getPartitioner().newInstance();
            } catch ( Throwable t ) {
                throw new RuntimeException( t );
            }

        }

        return partitionerInstance;
        
    }

	public Class getComparator() {
		return comparator;
	}

	public Job setComparator(Class comparator) {
		this.comparator = comparator;
		return this;
	}

    public SortComparator getComparatorInstance() {

        try {
            return (SortComparator)getComparator().newInstance();
        } catch ( Throwable t ) {
            throw new RuntimeException( t );
        }
        
    }

    public int getMaxChunks() { 
        return this.maxChunks;
    }

    public Job setMaxChunks( int maxChunks ) { 
        this.maxChunks = maxChunks;
        return this;
    }

    public Message getParameters() {
        return parameters;
    }

    public Job setParameters( Object... args ) {
        setParameters( new Message( args ) );
        return this;
    }
    
    public Job setParameters( Map parameters ) {
        setParameters( new Message( parameters ) );
        return this;
    }

    public Job setParameters( Message parameters ) {
        this.parameters = parameters;
        return this;
    }

    public String getOperation() {
        return operation;
    }

    public Job setOperation( String operation ) {
        this.operation = operation;
        return this;
    }

    public Report getReport() { 
        return this.report;
    }

    public Job setReport( Report report ) { 
        this.report = report;
        return this;
    }

    @Override
    public String toString() {

        //TODO: include ALL fields.
        return String.format( "%s (%s) for input %s and output %s ",
                              getDelegate().getName(),
                              getName(),
                              getInput(),
                              getOutput() );

    }

    /**
     * Convert this to an RPC message.
     */
    @Override
    public Message toMessage() {

        Message message = super.toMessage();

        message.put( "class",         getClass().getName() );
        message.put( "timestamp",     timestamp );
        message.put( "delegate",      delegate );
        message.put( "combiner",      combiner );
        message.put( "input",         input.getReferences() );
        message.put( "output",        output.getReferences() );
        message.put( "partitioner",   partitioner );
        message.put( "maxChunks",     maxChunks );
        message.put( "comparator",    comparator );
        message.put( "parameters",    parameters );
        message.put( "operation",     operation );
        message.put( "report",        report.toMessage() );

        return message;
        
    }

    @Override
    public void fromMessage( Message message ) {

        super.fromMessage( message );

        timestamp     = message.getLong( "timestamp" );
        delegate      = message.getClass( "delegate" );
        combiner      = message.getClass( "combiner" );
        input         = new Input( message.getList( "input" ) );
        output        = new Output( message.getList( "output" ) );
        partitioner   = message.getClass( "partitioner" );
        maxChunks     = message.getInt( "maxChunks" );
        comparator    = message.getClass( "comparator" );
        parameters    = new Message( message.getString( "parameters" ) );
        operation     = message.getString( "operation" );

        this.report = new Report();
        this.report.fromMessage( message.getMessage( "report" ) );
        
    }
    
}

