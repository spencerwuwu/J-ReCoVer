// https://searchcode.com/api/result/13445449/

package mw.cliquefind;

import java.io.IOException;
import java.util.ArrayList;

import mw.cliquefind.datatypes.Edge;
import mw.cliquefind.datatypes.WeightedEdge;
import mw.cliquefind.datatypes.WeightedVertex;
import mw.cliquefind.io.RecordOutputFormat;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;

public class S2JoinVertexDegreesJob extends Job {

	public static class MapWeightedVertex extends Mapper<NullWritable, WeightedVertex, IntWritable, WeightedEdge> {	
		@Override
		public void map(NullWritable k, WeightedVertex w, Context ctx) {
			
			//System.out.println("WeightedVertex: " + w.id + " " + w.weight);
			try {
				WeightedEdge we = new WeightedEdge();
				we.a = w.id;
				we.b = -1;
				we.weight = w.weight;
				ctx.write(new IntWritable(we.a), we);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	
	public static class MapEdge extends Mapper<NullWritable, Edge, IntWritable, WeightedEdge> {	
		@Override
		public void map(NullWritable k, Edge w, Context ctx) {
			try {
				WeightedEdge we = new WeightedEdge();
				we.a = w.a;
			 	we.b = w.b;
				we.weight = -1;
				ctx.write(new IntWritable(we.a), we);
				//System.out.println("Map Edge: " + we.a + "---(" + we.weight + ")--->" + we.b);
				
				WeightedEdge we2 = new WeightedEdge();
				we2.a = w.b;
				we2.b = w.a;
				we2.weight = -1;
				ctx.write(new IntWritable(we2.a), we2);
				//System.out.println("Map Edge: " + we2.a + "---(" + we2.weight + ")--->" + we2.b);
			} catch (Exception e) {
				e.printStackTrace();
			}
			
		}
	}
	
	public static class Reduce extends Reducer<IntWritable, WeightedEdge, NullWritable, WeightedEdge> {
		@Override
		public void reduce(IntWritable key, Iterable<WeightedEdge> vals, Context ctx) {
			
			long weight = 0;

			ArrayList<Integer> ints = new ArrayList<Integer>(); 
			
			int count = 0;
			for (WeightedEdge we: vals) {
				if (we.b == -1) {
					weight = we.weight;
				} else {
					ints.add(we.b);
				}
				count++;
			}
			if (count != weight+1)
				System.out.println("count sollte == weight+1 sein bei S2 reducer!!!!!!!!!!!!!!" + count + " <- count weight -> " + weight);
					
			for (Integer i: ints) {
				WeightedEdge out = new WeightedEdge();
				out.a = key.get();
				out.b = i.intValue();
				out.weight = weight;
				try {
					ctx.write(NullWritable.get(), out);
				} catch (Exception e) {
					e.printStackTrace();
				} 
				
			}
		}
	}
	
	public S2JoinVertexDegreesJob(Configuration conf, String jobName) throws IOException {
		super(conf, jobName);

		setJarByClass(getClass());

		setOutputFormatClass(RecordOutputFormat.class);
		
		setMapperClass(MapWeightedVertex.class);
		setMapOutputKeyClass(IntWritable.class);
		setMapOutputValueClass(WeightedEdge.class);
		
		setReducerClass(Reduce.class);
		setOutputKeyClass(NullWritable.class);
		setOutputValueClass(WeightedEdge.class);
	}

}

