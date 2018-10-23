// https://searchcode.com/api/result/124442606/


package eu.mosaic_cloud.benchmarks.prototype;


import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;

import com.google.common.base.Preconditions;


public final class Pipeline
		extends Object
{
	private Pipeline (final Pool pool, final CountDownLatch ready)
	{
		super ();
		this.pool = pool;
		this.ready = ready;
	}
	
	public final void join ()
			throws InterruptedException
	{
		this.pool.join ();
	}
	
	public final void shouldStop ()
	{
		this.pool.shouldStop ();
	}
	
	public final void start ()
			throws InterruptedException
	{
		this.pool.start ();
		this.ready.countDown ();
		this.ready.await ();
	}
	
	private final Pool pool;
	private final CountDownLatch ready;
	
	public static final Pipeline create (final BlockingQueueBuilder inboundQueue, final int inboundFanout, final BlockingQueueBuilder outboundQueue, final int outboundFanout, final int exchangeStages, final BlockingQueueBuilder exchangeQueue, final int exchangeFanout, final Unit.UnitBuilder unit, final Unit.ReducerBuilder reducer)
	{
		Preconditions.checkNotNull (inboundQueue);
		Preconditions.checkArgument (inboundFanout > 0);
		Preconditions.checkNotNull (outboundQueue);
		Preconditions.checkArgument (outboundFanout > 0);
		Preconditions.checkArgument (exchangeStages >= 0);
		if (exchangeStages > 0) {
			Preconditions.checkNotNull (exchangeQueue);
			Preconditions.checkArgument (exchangeFanout > 0);
		} else {
			Preconditions.checkArgument (exchangeQueue == null);
			Preconditions.checkArgument (exchangeFanout == 0);
		}
		Preconditions.checkNotNull (unit);
		final BlockingQueue<Unit> inbound = inboundQueue.build ();
		final BlockingQueue<Unit> outbound = (exchangeStages > 0) ? outboundQueue.build () : inbound;
		final CountDownLatch ready = new CountDownLatch (inboundFanout + outboundFanout + exchangeStages * exchangeFanout + 1);
		final Pool pool = new Pool ();
		final Pipeline pipeline = new Pipeline (pool, ready);
		for (int index = 0; index < inboundFanout; index++)
			new InboundThread (pipeline, index, inbound, ready, unit);
		{
			BlockingQueue<Unit> inputs = inbound;
			for (int stage = 0; stage < exchangeStages; stage++) {
				final BlockingQueue<Unit> outputs = (stage < (exchangeStages - 1)) ? exchangeQueue.build () : outbound;
				for (int index = 0; index < exchangeFanout; index++)
					new ExchangeThread (pipeline, stage, index, inputs, outputs, ready);
				inputs = outputs;
			}
		}
		for (int index = 0; index < outboundFanout; index++)
			new OutboundThread (pipeline, index, outbound, ready, (reducer != null) ? reducer.build () : null);
		return (pipeline);
	}
	
	public static final class Arguments
			extends Object
			implements
				eu.mosaic_cloud.benchmarks.store.Arguments
	{
		public Integer buffer;
		public Integer fanout;
		public Integer stages;
		public Float timeout;
	}
	
	public static final class ArrayBlockingQueueBuilder
			extends Object
			implements
				BlockingQueueBuilder
	{
		public ArrayBlockingQueueBuilder (final int size, final boolean fair)
		{
			super ();
			Preconditions.checkArgument (size > 0);
			this.size = size;
			this.fair = fair;
		}
		
		public final ArrayBlockingQueue<Unit> build ()
		{
			return (new ArrayBlockingQueue<Unit> (this.size, this.fair));
		}
		
		public final boolean fair;
		public final int size;
	}
	
	public static interface BlockingQueueBuilder
	{
		public abstract BlockingQueue<Unit> build ();
	}
	
	public static final class ExchangeThread
			extends Pipeline.Thread
	{
		public ExchangeThread (final Pipeline pipeline, final int stage, final int index, final BlockingQueue<Unit> inputs, final BlockingQueue<Unit> outputs, final CountDownLatch ready)
		{
			pipeline.super ();
			Preconditions.checkArgument ((stage >= 0) && (index >= 0));
			Preconditions.checkNotNull (inputs);
			Preconditions.checkNotNull (outputs);
			Preconditions.checkNotNull (ready);
			this.inputs = inputs;
			this.outputs = outputs;
			this.ready = ready;
			this.setName (String.format ("%s#%02d#%02d", this.getClass ().getSimpleName (), stage, index));
			// System.err.format ("%08x -> %s -> %08x\n", System.identityHashCode (this.inputs), this.getName (), System.identityHashCode (this.outputs));
		}
		
		@Override
		public final void run ()
		{
			this.ready.countDown ();
			try {
				this.ready.await ();
			} catch (final InterruptedException exception) {
				throw (new IllegalStateException (exception));
			}
			while (this.shouldRun ())
				try {
					final Unit unit = this.inputs.take ();
					unit.dequeued ();
					unit.enqueued ();
					this.outputs.put (unit);
				} catch (final InterruptedException exception) {
					throw (new IllegalStateException (exception));
				}
		}
		
		private final BlockingQueue<Unit> inputs;
		private final BlockingQueue<Unit> outputs;
		private final CountDownLatch ready;
	}
	
	public static final class InboundThread
			extends Pipeline.Thread
	{
		public InboundThread (final Pipeline pipeline, final int index, final BlockingQueue<Unit> inbound, final CountDownLatch ready, final Unit.UnitBuilder builder)
		{
			pipeline.super ();
			Preconditions.checkArgument (index >= 0);
			Preconditions.checkNotNull (inbound);
			Preconditions.checkNotNull (ready);
			Preconditions.checkNotNull (builder);
			this.inbound = inbound;
			this.ready = ready;
			this.builder = builder;
			this.setName (String.format ("%s#%02d", this.getClass ().getSimpleName (), index));
			// System.err.format ("%s -> %08x\n", this.getName (), System.identityHashCode (this.inbound));
		}
		
		@Override
		public final void run ()
		{
			this.ready.countDown ();
			try {
				this.ready.await ();
			} catch (final InterruptedException exception) {
				throw (new IllegalStateException (exception));
			}
			while (this.shouldRun ())
				try {
					final Unit unit = this.builder.build ();
					unit.created ();
					unit.enqueued ();
					this.inbound.put (unit);
				} catch (final InterruptedException exception) {
					throw (new IllegalStateException (exception));
				}
		}
		
		private final Unit.UnitBuilder builder;
		private final BlockingQueue<Unit> inbound;
		private final CountDownLatch ready;
	}
	
	public static final class OutboundThread
			extends Pipeline.Thread
	{
		public OutboundThread (final Pipeline pipeline, final int index, final BlockingQueue<Unit> outbound, final CountDownLatch ready, final Unit.Reducer reducer)
		{
			pipeline.super ();
			Preconditions.checkArgument (index >= 0);
			Preconditions.checkNotNull (outbound);
			Preconditions.checkNotNull (ready);
			this.outbound = outbound;
			this.ready = ready;
			this.reducer = reducer;
			this.setName (String.format ("%s#%02d", this.getClass ().getSimpleName (), index));
			// System.err.format ("%08x -> %s\n", System.identityHashCode (this.outbound), this.getName ());
		}
		
		@Override
		public final void run ()
		{
			this.ready.countDown ();
			try {
				this.ready.await ();
			} catch (final InterruptedException exception) {
				throw (new IllegalStateException (exception));
			}
			while (this.shouldRun ())
				try {
					final Unit unit = this.outbound.take ();
					unit.completed ();
					if (this.reducer != null)
						this.reducer.reduce (unit);
				} catch (final InterruptedException exception) {
					throw (new IllegalStateException (exception));
				}
		}
		
		private final BlockingQueue<Unit> outbound;
		private final CountDownLatch ready;
		private final Unit.Reducer reducer;
	}
	
	public static final class Scenario
			extends Object
			implements
				eu.mosaic_cloud.benchmarks.store.Scenario
	{
		public Integer[] buffer;
		public Integer[] fanout;
		public Integer[] stages;
		public Float timeout;
		public Boolean warmup;
	}
	
	public abstract class Thread
			extends Pool.Thread
	{
		protected Thread ()
		{
			Pipeline.this.pool.super ();
		}
	}
}

