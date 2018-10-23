// https://searchcode.com/api/result/54362882/

package dbmm;

import java.io.*;
import java.util.*;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.GenericOptionsParser;

public class KMeans {

	private static int N = 2;
	private static String clusters = "clusters.txt";

	public static class GroupMapper extends Mapper<LongWritable, Text, IntWritable, Text>{
      
		public void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {

			String pointStr = value.toString();

			StringTokenizer pointTok = new StringTokenizer(pointStr);
	System.out.println("<<<" + pointStr + ">>>");
			if (pointStr.equals("")) return;

			int pointId = Integer.parseInt(pointTok.nextToken());
			double[] pointData = new double[N];
			int i = 0;

			while (pointTok.hasMoreTokens()) {
				pointData[i] = Double.parseDouble(pointTok.nextToken());
				i++;
			}

			Configuration conf = context.getConfiguration();
			FileSystem hdfs = FileSystem.get(conf);
			FSDataInputStream fis = hdfs.open(new Path(clusters));
			BufferedReader reader = new BufferedReader(new InputStreamReader(fis));
			String centerStr = null;

			double minDist = 1000000.00;
			int minId = -1;

			while ((centerStr = reader.readLine()) != null) {
				if (centerStr.equals("")) break;

				StringTokenizer centerTok = new StringTokenizer(centerStr);

				//System.out.println("<<<" + centerStr + ">>>");
				int centerId = Integer.parseInt(centerTok.nextToken());
				double[] centerData = new double[N];
				int j = 0;

				while (centerTok.hasMoreTokens()) {
					centerData[j] = Double.parseDouble(centerTok.nextToken());
					++j;
				}

				double dist = 0.00;

				for (int k = 0; k < N; ++k) {
					double d = pointData[k] - centerData[k];
					dist += d * d;
				}

				if (dist < minDist) {
					minDist = dist;
					minId = centerId;
				}
			}

			reader.close();
			fis.close();

			IntWritable outId = new IntWritable(minId);
			Text outPointStr = new Text(pointStr);

			context.write(outId, outPointStr);
		}
	}

	public static class IdentityReducer extends Reducer<IntWritable, Text, IntWritable, Text> {

		public void reduce(IntWritable key, Iterable<Text> values, Context context) throws IOException, InterruptedException {

			for (Text val : values) {

				context.write(key, val);
			}
		}
	}

	public static class AverageReducer extends Reducer<IntWritable, Text, IntWritable, Text> {

		public void reduce(IntWritable key, Iterable<Text> values, Context context) throws IOException, InterruptedException {

			double[] avgData = new double[N];
			int cnt = 0;

			for (Text val : values) {

				String pointStr = val.toString();
				//System.out.println("<<<" + pointStr + ">>>");
				StringTokenizer pointTok = new StringTokenizer(pointStr);

				int centerId = Integer.parseInt(pointTok.nextToken());
				int j = 0;

				while (pointTok.hasMoreTokens()) {
					avgData[j] += Double.parseDouble(pointTok.nextToken());
					++j;
				}

				++cnt;
			}

			for (int k = 0; k < N; ++k) {
				avgData[k] /= cnt;
				avgData[k] = Math.floor(avgData[k] * 1000) / 1000.00;
			}

			String avgStr = "";

			for (int k = 0; k < N; ++k) {
				avgStr += "" + avgData[k] + ((k < N - 1) ? " " : "");
			}

			IntWritable outId = new IntWritable(key.get());
			Text outAvgStr = new Text(avgStr);
			context.write(outId, outAvgStr);
		}
	}

	public static void main(String[] args) throws Exception {

		Configuration conf = new Configuration();
		String[] otherArgs = new GenericOptionsParser(conf, args).getRemainingArgs();

		if (otherArgs.length != 4) {
			System.err.println("Usage: kmeans points-file initial-clusters space-size num-iter");
			System.exit(2);
		}

		N = Integer.parseInt(otherArgs[2]);
		int nIter = Integer.parseInt(otherArgs[3]);

		// First iteration
		Job job = new Job(conf, "kmeans clustering - " + 1);
		job.setJarByClass(KMeans.class);
		job.setMapperClass(GroupMapper.class);
		//job.setCombinerClass(AverageReducer.class);
		job.setReducerClass(AverageReducer.class);
		job.setOutputKeyClass(IntWritable.class);
		job.setOutputValueClass(Text.class);
		clusters = otherArgs[1];
		FileInputFormat.addInputPath(job, new Path(otherArgs[0]));
		FileOutputFormat.setOutputPath(job, new Path("kmeans-step-1"));
		job.waitForCompletion(true);

		for (int i = 1; i <= nIter - 1; ++i) {

			job = new Job(conf, "kmeans clustering - " + (i + 1));
			job.setJarByClass(KMeans.class);
			job.setMapperClass(GroupMapper.class);
			//job.setCombinerClass(AverageReducer.class);
			job.setReducerClass(AverageReducer.class);
			job.setOutputKeyClass(IntWritable.class);
			job.setOutputValueClass(Text.class);
			clusters = "kmeans-step-" + (i) + "/part-r-00000";
			FileInputFormat.addInputPath(job, new Path(otherArgs[0]));
			FileOutputFormat.setOutputPath(job, new Path("kmeans-step-" + (i + 1) + ""));
			job.waitForCompletion(true);
		}
	}
}


