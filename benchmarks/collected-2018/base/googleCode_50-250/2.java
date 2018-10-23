// https://searchcode.com/api/result/13445450/

package mw.cliquefind;

import java.io.IOException;
import mw.cliquefind.datatypes.Edge;
import mw.cliquefind.datatypes.Triangle;
import mw.cliquefind.io.RecordOutputFormat;
import mw.cliquefind.io.TriangleInputFormat;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.mapreduce.Counter;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;

public class S6FindTrussJob extends Job {
	
	public static enum MyGroup {
		WEGGEFALLEN_COUNT
	}

	public static class Map extends Mapper<NullWritable, Triangle, Edge, LongWritable> {
		@Override
		public void map(NullWritable k, Triangle t, Context ctx) {
			Edge e1 = new Edge(t.a, t.b);
			Edge e2 = new Edge(t.b, t.c);
			Edge e3 = new Edge(t.c, t.a);

			try {
				ctx.write(e1, new LongWritable(1));
				ctx.write(e2, new LongWritable(1));
				ctx.write(e3, new LongWritable(1));
			} catch (Exception e) {
				e.printStackTrace();
			} 
		}
	}
	
	public static class Combiner extends Reducer<Edge, LongWritable, Edge, LongWritable> {
		@Override
		public void reduce(Edge k, Iterable<LongWritable> vals, Context ctx) {
			long count = 0;
			for (LongWritable lw: vals) {
				count = count + lw.get();
			}
		
			try {
				ctx.write(k, new LongWritable(count));
			} catch (Exception e) {
				e.printStackTrace();
			} 
		}
	}
	
	public static class Reduce extends Reducer<Edge, LongWritable, NullWritable, Edge> {
		@Override
		public void reduce(Edge k, Iterable<LongWritable> vals, Context ctx) {
			long count = 0;
			for (LongWritable lw: vals) {
				count = count + lw.get();
			}
			
			Counter ct = ctx.getCounter(MyGroup.WEGGEFALLEN_COUNT);
			
			int n = ctx.getConfiguration().getInt("n", 5);
			if (count >= n-2) {
				try {
					ctx.write(NullWritable.get(), k);
				} catch (Exception e) {
					e.printStackTrace();
				} 
			} else {
				ct.increment(1);
			}
		}
	}
	
	
	
	public S6FindTrussJob(Configuration conf, String jobName) throws IOException {
		super(conf, jobName);
	
		setJarByClass(getClass());
		
		setInputFormatClass(TriangleInputFormat.class);
		setOutputFormatClass(RecordOutputFormat.class);
		
		setMapperClass(Map.class);
		setMapOutputKeyClass(Edge.class);
		setMapOutputValueClass(LongWritable.class);
		
		setCombinerClass(Combiner.class);
		
		setReducerClass(Reduce.class);
		setOutputKeyClass(NullWritable.class);
		setOutputValueClass(Edge.class);
	}
	
}

