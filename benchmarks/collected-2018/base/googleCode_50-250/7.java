// https://searchcode.com/api/result/13445448/

package mw.cliquefind;

import java.io.IOException;
import java.util.ArrayList;

import mw.cliquefind.datatypes.Edge;
import mw.cliquefind.datatypes.Triangle;
import mw.cliquefind.io.EdgeInputFormat;
import mw.cliquefind.io.RecordOutputFormat;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;

public class S4ListTriangleCandidatesJob extends Job {
	
	public static enum TriangleCountS4 {
		COUNT_TRIANGLES, COUNT_MAPCALLS
	}

	public static class Map extends Mapper<NullWritable, Edge, IntWritable, IntWritable> {
		@Override
		public void map(NullWritable k, Edge w, Context ctx) {
			try {
				ctx.write(new IntWritable(w.a), new IntWritable(w.b));
			} catch (Exception e) {
				e.printStackTrace();
			} 
			
			ctx.getCounter(TriangleCountS4.COUNT_MAPCALLS).increment(1);
		
		}
	}
	
	public static class Reduce extends Reducer<IntWritable, IntWritable, NullWritable, Triangle> {
		@Override
		public void reduce(IntWritable k, Iterable<IntWritable> vals, Context ctx) {
			ArrayList<Integer> al = new ArrayList<Integer>();
			for (IntWritable iw: vals) {
				int cur = iw.get();
				al.add(cur);
			}

			
			for (int i = 0; i < al.size()-1; i++) {
				for (int j = i+1; j < al.size(); j++) {
					Triangle t = new Triangle(k.get(), al.get(i), al.get(j));
					ctx.getCounter(TriangleCountS4.COUNT_TRIANGLES).increment(1);
					try {
						ctx.write(NullWritable.get(), t);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}
	}
	
	public S4ListTriangleCandidatesJob(Configuration conf, String jobName) throws IOException {
		super(conf, jobName);
	
		setJarByClass(getClass());
		
		setInputFormatClass(EdgeInputFormat.class);
		setOutputFormatClass(RecordOutputFormat.class);
		
		setMapperClass(Map.class);
		setMapOutputKeyClass(IntWritable.class);
		setMapOutputValueClass(IntWritable.class);
		
		setReducerClass(Reduce.class);
		setOutputKeyClass(NullWritable.class);
		setOutputValueClass(Triangle.class);
	}

}

