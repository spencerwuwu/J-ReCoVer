// https://searchcode.com/api/result/13445444/

package mw.cliquefind;

import java.io.IOException;

import mw.cliquefind.datatypes.Edge;
import mw.cliquefind.datatypes.WeightedEdge;
import mw.cliquefind.io.RecordOutputFormat;
import mw.cliquefind.io.WeightedEdgeInputFormat;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;

public class S3GraphDirectionJob extends Job {

	public static class Map extends Mapper<NullWritable, WeightedEdge, Edge, WeightedEdge> {
		@Override
		public void map(NullWritable k, WeightedEdge w, Context ctx) {
			
			Edge key = new Edge();
			WeightedEdge we = new WeightedEdge();
			we.a = w.a; we.b = w.b; we.weight = w.weight;
			
			key.a = w.a;
			key.b = w.b;
			
			try {
				ctx.write(key, we);
			} catch (Exception e) {
				e.printStackTrace();
			} 
		}
	}
	
	public static class Reduce extends Reducer<Edge, WeightedEdge, NullWritable, Edge> {
		@Override
		public void reduce(Edge k, Iterable<WeightedEdge> vals, Context ctx) {
			
			WeightedEdge weA = null;
			WeightedEdge weB = null;
			
			//int a = 0, b = 0;
			
			int i = 0;
			// gibt nur 2
			for (WeightedEdge we: vals) {
				//a = we.a; b = we.b;
				if (i == 0) {
					weA = new WeightedEdge();
					weA.a = we.a; weA.b = we.b; weA.weight = we.weight;
				}
				else {
					weB = new WeightedEdge();
					weB.a = we.a; weB.b = we.b; weB.weight = we.weight;
				}
				i++;
			}
		
			if (i != 2)
				System.out.println("S3 - Reducer - FEHLER!!! " + i + " ");// + weA.weight + " " + weB.weight + " ::: " + k.a + "->" + k.b);
			
			try {
				Edge tmp = null;
				if (weA.weight < weB.weight) {
					tmp = new Edge(weA.a, weA.b);
					ctx.write(NullWritable.get(), tmp);
				} else if (weA.weight > weB.weight){
					tmp = new Edge(weB.a, weB.b);
					ctx.write(NullWritable.get(), tmp);
				} else {
					if (weA.a <= weA.b)
						tmp = new Edge(weA.a, weA.b);
					else
						tmp = new Edge(weA.b, weA.a);
					ctx.write(NullWritable.get(), tmp);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	
	public S3GraphDirectionJob(Configuration conf, String jobName) throws IOException {
		super(conf, jobName);
		
		setJarByClass(getClass());
		
		setInputFormatClass(WeightedEdgeInputFormat.class);
		setOutputFormatClass(RecordOutputFormat.class);
		
		setMapperClass(Map.class);
		setMapOutputKeyClass(Edge.class);
		setMapOutputValueClass(WeightedEdge.class);	
		
		setReducerClass(Reduce.class);
		setOutputKeyClass(NullWritable.class);
		setOutputValueClass(Edge.class);
		
		//setGroupingComparatorClass(GruppiererS3.class);
		//setSortComparatorClass(GruppiererS3.class);
	}

}

