// https://searchcode.com/api/result/96335780/

import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.StringTokenizer;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

/**
 * Code solving triangle friendship problem via
 * Hadoop MapReduce framework.
 * <br/><br/>
 * <b>Input:</b> Lines of people's friends
 * <br/>Format: [person] (tab) [friend], [friend] ...
 * <br/><br/>
 * <b>Output:</b> Lines of people consisting a "friendship triangle"
 * <br/>Format: [person], [person], [person]
 * <br/><br/>For simplicity, we use numbers to indicate people,
 * and assume input file is sorted.
 * @author Belmen
 *
 */
public class TriangleFriendship extends Configured implements Tool {

	public static class Map extends Mapper<LongWritable, Text, Text, LongWritable> {

		private Text mKey = new Text();
		private LongWritable mValue = new LongWritable();
		
		/**
		 * Map phase: output the number of "effective edges" contributed to a triangle
		 */
		@Override
		protected void map(LongWritable key, Text value, Context context)
				throws IOException, InterruptedException {
			String line = value.toString();
			StringTokenizer tokenizer = new StringTokenizer(line);
			if(tokenizer.hasMoreTokens()) {
				// Apex: the center of a friendship,
				// other people have friendship with him
				int apex = Integer.parseInt(tokenizer.nextToken());
				if (!tokenizer.hasMoreTokens()) {
					throw new RuntimeException("invalid intermediate line " + line);
				}
				String[] nodes = tokenizer.nextToken().split(",");
				int size = nodes.length;
				int[] triangle = new int[3];
				
				/*
				 * Enumerate all possible triangles containing the apex.
				 * The edges starting from the apex contribute to the triangle,
				 * so output the number of contributing edges to the triangle from
				 * this friendship line.
				 */
				for(int i = 0; i < size; i++) {
					for(int j = i + 1; j < size; j++) {
						int node1 = Integer.parseInt(nodes[i]);
						int node2 = Integer.parseInt(nodes[j]);
						
						triangle[0] = apex;
						triangle[1] = node1;
						triangle[2] = node2;
						
						// Count only when node > apex so edges won't count twice
						int count = (apex < node1 ? 1 : 0) + (apex < node2 ? 1 : 0);
						// Make sure we output the same triangle
						Arrays.sort(triangle);
						// We denote the triangle by vertices splitted by commas,
						// like 1,3,6
						StringBuilder sb = new StringBuilder();
						for(int k = 0; k < 3; k++) {
							sb.append(triangle[k]);
							if(k != 2) {
								sb.append(',');
							}
						}
						// Output the triangle and edge count to reducer
						mKey.set(sb.toString());
						mValue.set(count);
						context.write(mKey, mValue);
					}
				}
			}
		}
	}
	
	public static class Reduce extends Reducer<Text, LongWritable, Text, NullWritable> {

		/**
		 * Reduce phase: sum up the number of edges of the triangle.
		 * If there are 3 edges, then it is a complete triangle and 
		 * we output it.
		 */
		@Override
		protected void reduce(Text key, Iterable<LongWritable> values,
				Context context) throws IOException, InterruptedException {
			Iterator<LongWritable> iter = values.iterator();
			int sum = 0;
			while(iter.hasNext()) {
				sum += iter.next().get();
			}
			if(sum == 3) { // Output if it has 3 edges
				context.write(key, null);
			}
		}

	}
	
	@Override
	public int run(String[] args) throws Exception {
		Configuration conf = getConf();
		Job job = new Job(conf);
		job.setJobName("TriangleFriendship");
		
		job.setMapOutputKeyClass(Text.class);
		job.setMapOutputValueClass(LongWritable.class);
		
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(NullWritable.class);
		
		job.setMapperClass(Map.class);
		job.setReducerClass(Reduce.class);
		
		FileInputFormat.addInputPath(job, new Path(args[0]));
        FileOutputFormat.setOutputPath(job, new Path(args[1]));
		
        job.waitForCompletion(true);
        
		return 0;
	}

	public static void main(String[] args) throws Exception {
		int res = ToolRunner.run(new Configuration(), new TriangleFriendship(), args);
        System.exit(res);
	}

}

