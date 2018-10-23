// https://searchcode.com/api/result/13445446/

package mw.cliquefind;

import java.io.IOException;
import java.util.ArrayList;
import mw.cliquefind.datatypes.Edge;
import mw.cliquefind.datatypes.Triangle;
import mw.cliquefind.io.RecordOutputFormat;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;

public class S5FindTrianglesJob extends Job {

	public static enum TriangleCountS5 {
		COUNT_TRIANGLES
	}
	
	public static class MapTriangle extends Mapper<NullWritable, Triangle, Edge, Triangle> {	
		@Override
		public void map(NullWritable k, Triangle w, Context ctx) {
			try {
				Triangle t = new Triangle(w.a, w.b, w.c);
				ctx.write(new Edge(w.b, w.c), t);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	
	public static class MapEdge extends Mapper<NullWritable, Edge, Edge, Triangle> {	
		@Override
		public void map(NullWritable k, Edge w, Context ctx) {
			Triangle t = new Triangle(-1, -1, -1);
			try {
				ctx.write(new Edge(w.a, w.b), t);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	public static class Reduce extends Reducer<Edge, Triangle, NullWritable, Triangle> {
		@Override
		public void reduce(Edge key, Iterable<Triangle> vals, Context ctx) {
			
			ArrayList<Triangle> triangles = new ArrayList<Triangle>();
			boolean isContained = false;

			for(Triangle tri : vals) {
				if (tri.c == -1) { // im prinzip sind alle werte -1 oder gar keiner
					isContained = true;
				} else {
					triangles.add(new Triangle(tri.a, tri.b, tri.c));
				}
			}
			
			if (!isContained)
				return;

			for (Triangle t : triangles) {
				
				ctx.getCounter(TriangleCountS5.COUNT_TRIANGLES).increment(1);
				try {
					ctx.write(NullWritable.get(), t);
				} catch (Exception e) {
					e.printStackTrace();
				} 

			}
		}
	}
	
	public S5FindTrianglesJob(Configuration conf, String jobName) throws IOException {
		super(conf, jobName);
		
		setJarByClass(getClass());
		
		setOutputFormatClass(RecordOutputFormat.class);
		
		setMapperClass(MapTriangle.class);
		setMapOutputKeyClass(Edge.class);
		setMapOutputValueClass(Triangle.class);
		
		setReducerClass(Reduce.class);
		setOutputKeyClass(NullWritable.class);
		setOutputValueClass(Triangle.class);
	}

	
}

