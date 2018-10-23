// https://searchcode.com/api/result/13445445/

package mw.cliquefind;

import java.io.IOException;

import mw.cliquefind.datatypes.Edge;
import mw.cliquefind.datatypes.WeightedVertex;
import mw.cliquefind.io.EdgeInputFormat;
import mw.cliquefind.io.RecordOutputFormat;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.Job;


public class S1CalculateVertexDegreesJob extends Job {
	
	public static enum FriendCount {
		COUNT_NODES, COUNT_FRIENDSHIPS
	}

	public static class Map extends Mapper<NullWritable, Edge, IntWritable, LongWritable> {
		@Override
		public void map(NullWritable k, Edge w, Context ctx) {
			
			// statistics
			ctx.getCounter(FriendCount.COUNT_FRIENDSHIPS).increment(2);
			
			try {
				ctx.write(new IntWritable(w.a), new LongWritable(1));
				ctx.write(new IntWritable(w.b), new LongWritable(1));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	public static class Combiner extends Reducer<IntWritable, LongWritable, IntWritable, LongWritable> {
		@Override
		public void reduce(IntWritable k, Iterable<LongWritable> vals, Context ctx) {
			long count = 0;
			for (LongWritable lw: vals) {
				count = count + lw.get();
			}
		
			try {
				ctx.write(new IntWritable(k.get()), new LongWritable(count));
			} catch (Exception e) {
				e.printStackTrace();
			} 
		}
	}
	
	public static class Reduce extends Reducer<IntWritable, LongWritable, NullWritable, WeightedVertex> {
		@Override
		public void reduce(IntWritable k, Iterable<LongWritable> w, Context ctx) {
			long count = 0;
			for (LongWritable lw: w) {
				count = count + lw.get();
			}
			
			// statistics
			ctx.getCounter(FriendCount.COUNT_NODES).increment(1);
			
			WeightedVertex wv = new WeightedVertex();
			wv.id = k.get();
			wv.weight = count;
			
			try {
				ctx.write(NullWritable.get(), wv);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	public S1CalculateVertexDegreesJob(Configuration conf, String jobName) throws IOException {
		super(conf, jobName);

		setJarByClass(getClass());
		
		setInputFormatClass(EdgeInputFormat.class);
		setOutputFormatClass(RecordOutputFormat.class);
		
		setMapperClass(Map.class);
		setMapOutputKeyClass(IntWritable.class);
		setMapOutputValueClass(LongWritable.class);
		
		setCombinerClass(Combiner.class);
		
		setReducerClass(Reduce.class);
		setOutputKeyClass(NullWritable.class);
		setOutputValueClass(WeightedVertex.class);
	}
}

