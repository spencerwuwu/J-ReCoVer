// https://searchcode.com/api/result/124442607/


package eu.mosaic_cloud.benchmarks.prototype;


import java.util.LinkedList;
import java.util.concurrent.ConcurrentLinkedQueue;


public final class Unit
		extends Object
{
	public Unit (final Observer observer)
	{
		super ();
		this.observer = observer;
	}
	
	public final void completed ()
	{
		this.completed = System.nanoTime ();
		if (this.observer != null)
			this.observer.completed (this);
	}
	
	public final void created ()
	{
		this.created = System.nanoTime ();
		if (this.observer != null)
			this.observer.created (this);
	}
	
	public final void dequeued ()
	{
		if (this.observer != null)
			this.observer.dequeued (this);
	}
	
	public final void enqueued ()
	{
		if (this.observer != null)
			this.observer.enqueued (this);
	}
	
	public final float latency ()
	{
		return ((this.completed - this.created) / 1000f / 1000f / 1000f);
	}
	
	private long completed;
	private long created;
	private final Observer observer;
	
	public static final class CompletionQueueingReducer
			extends Object
			implements
				Reducer
	{
		public CompletionQueueingReducer ()
		{
			this (new ConcurrentLinkedQueue<Unit> ());
		}
		
		public CompletionQueueingReducer (final ConcurrentLinkedQueue<Unit> queue)
		{
			super ();
			this.queue = queue;
		}
		
		public final void reduce (final Unit unit)
		{
			this.queue.add (unit);
		}
		
		public final ConcurrentLinkedQueue<Unit> queue;
	}
	
	public static final class DefaultReducerBuilder<_Reducer_ extends Reducer>
			extends Object
			implements
				ReducerBuilder
	{
		public DefaultReducerBuilder (final Class<_Reducer_> reducerClass)
		{
			super ();
			this.reducerClass = reducerClass;
			this.reducers = new LinkedList ();
		}
		
		public final Reducer build ()
		{
			try {
				final _Reducer_ reducer = this.reducerClass.newInstance ();
				synchronized (this.reducers) {
					this.reducers.add (reducer);
				}
				return (reducer);
			} catch (final Exception exception) {
				throw (new IllegalStateException (exception));
			}
		}
		
		public final Class<_Reducer_> reducerClass;
		public final LinkedList<_Reducer_> reducers;
	}
	
	public static final class DefaultUnitBuilder
			extends Object
			implements
				UnitBuilder
	{
		public DefaultUnitBuilder ()
		{
			this (null);
		}
		
		public DefaultUnitBuilder (final Observer observer)
		{
			super ();
			this.observer = observer;
		}
		
		public final Unit build ()
		{
			return (new Unit (this.observer));
		}
		
		public final Observer observer;
	}
	
	public static interface Observer
	{
		public abstract void completed (final Unit unit);
		
		public abstract void created (final Unit unit);
		
		public abstract void dequeued (final Unit unit);
		
		public abstract void enqueued (final Unit unit);
	}
	
	public static interface Reducer
	{
		public abstract void reduce (final Unit unit);
	}
	
	public static interface ReducerBuilder
	{
		public abstract Reducer build ();
	}
	
	public static final class StatisticsReducer
			extends Object
			implements
				Reducer
	{
		public StatisticsReducer ()
		{
			super ();
			this.count = 0;
			this.latencySum1 = 0;
			this.latencySum2 = 0;
			this.latencyMin = Double.MAX_VALUE;
			this.latencyMax = Double.MIN_VALUE;
		}
		
		public final Outcome outcome ()
		{
			final Outcome outcome = new Outcome ();
			outcome.count = this.count;
			outcome.latencySum1 = this.latencySum1;
			outcome.latencySum2 = this.latencySum2;
			outcome.latencyAvg = this.latencySum1 / this.count;
			outcome.latencyStddev = Math.sqrt (this.count * this.latencySum2 - this.latencySum1 * this.latencySum1) / this.count;
			outcome.latencyMin = this.latencyMin;
			outcome.latencyMax = this.latencyMax;
			return (outcome);
		}
		
		public final void reduce (final Iterable<StatisticsReducer> reducers)
		{
			for (final StatisticsReducer reducer : reducers)
				this.reduce (reducer);
		}
		
		public final void reduce (final StatisticsReducer reducer)
		{
			this.count += reducer.count;
			this.latencySum1 += reducer.latencySum1;
			this.latencySum2 += reducer.latencySum2;
			if (this.latencyMin > reducer.latencyMin)
				this.latencyMin = reducer.latencyMin;
			if (this.latencyMax < reducer.latencyMax)
				this.latencyMax = reducer.latencyMax;
		}
		
		public final void reduce (final Unit unit)
		{
			this.count += 1;
			final float latency = unit.latency ();
			this.latencySum1 += latency;
			this.latencySum2 += latency * latency;
			if (this.latencyMin > latency)
				this.latencyMin = latency;
			if (this.latencyMax < latency)
				this.latencyMax = latency;
		}
		
		public long count;
		public double latencyMax;
		public double latencyMin;
		public double latencySum1;
		public double latencySum2;
		
		public static final class Outcome
				extends Object
				implements
					eu.mosaic_cloud.benchmarks.store.Outcome
		{
			public Long count;
			public Double elapsed;
			public Double latencyAvg;
			public Double latencyMax;
			public Double latencyMin;
			public Double latencyStddev;
			public Double latencySum1;
			public Double latencySum2;
			public Double throughput;
		}
	}
	
	public static interface UnitBuilder
	{
		public abstract Unit build ();
	}
}

