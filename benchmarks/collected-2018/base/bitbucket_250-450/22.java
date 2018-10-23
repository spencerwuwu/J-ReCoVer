// https://searchcode.com/api/result/54362943/

package drivers.bovw.micc.unifi.it;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.apache.hadoop.util.GenericOptionsParser;
import org.hsqldb.lib.HashMap;

import types.bovw.micc.unifi.it.WritableSIFTKeypoint;
import formats.bovw.micc.unifi.it.SIFTKeypointsInputFormat;

public class KMeansDriver {

	public static class GroupMapper extends Mapper<Text, WritableSIFTKeypoint, Text, WritableSIFTKeypoint>{
		
		private Text outKey = new Text();
		private WritableSIFTKeypoint outValue = new WritableSIFTKeypoint();
		
		public void map(Text key, WritableSIFTKeypoint value, Context context) throws IOException, InterruptedException {

			String clusters = context.getConfiguration().get("clusters");
			int N = Integer.parseInt(context.getConfiguration().get("N"));
			
			float[] pointData = value.getData();

			Configuration conf = context.getConfiguration();

			float minDist = 1000000.0f;
			String minId = "?";
			
			Map<String, float[]> centers = readClusterCenters(clusters, conf);

			for (String centerId : centers.keySet()) {

				float[] centerData = centers.get(centerId);
				
				float dist = 0.0f;

				for (int k = 0; k < N; ++k) {
					float d = pointData[k] - centerData[k];
					dist += d * d;
				}

				if (dist < minDist) {
					minDist = dist;
					minId = centerId;
				}
			}
			
			outKey.set(minId);
			outValue.setX(value.getX());
			outValue.setY(value.getY());
			outValue.setScale(value.getScale());
			outValue.setOrientation(value.getOrientation());
			outValue.setData(value.getData());

			context.write(outKey, outValue);
		}
	}

	
	/*public static class IdentityReducer extends Reducer<Text, WritableSIFTKeypoint, Text, WritableSIFTKeypoint> {

		public void reduce(Text key, Iterable<WritableSIFTKeypoint> values, Context context) throws IOException, InterruptedException {

			for (WritableSIFTKeypoint val : values) {

				context.write(key, val);
			}
		}
	}*/
	

	public static class AverageReducer extends Reducer<Text, WritableSIFTKeypoint, Text, Text> {

		private Text outKey = new Text();
		private Text outValue = new Text();
		
		public void reduce(Text key, Iterable<WritableSIFTKeypoint> values, Context context) throws IOException, InterruptedException {
			
			int N = Integer.parseInt(context.getConfiguration().get("N"));
			
			float[] avgData = new float[N];
			int cnt = 0;

			for (WritableSIFTKeypoint val : values) {

				float[] pointData = val.getData();
				
				for (int j = 0; j < N; ++j) {
					avgData[j] += pointData[j];
				}

				++cnt;
			}

			for (int k = 0; k < N; ++k) {
				avgData[k] /= cnt;
				avgData[k] = (float)(Math.floor(avgData[k] * 1000) / 1000.00);
			}

			String avgStr = "";

			for (int k = 0; k < N; ++k) {
				avgStr += "" + avgData[k] + ((k < N - 1) ? " " : "");
			}

			outKey.set(key.toString());
			outValue.set(avgStr);
			
			context.write(outKey, outValue);
		}
	}

	public static void main(String[] args) throws Exception {

		try {
			
			Configuration conf = new Configuration();
			String[] otherArgs = new GenericOptionsParser(conf, args).getRemainingArgs();

			if (otherArgs.length != 4) {
				System.err.println("Usage: kmeans points-file initial-clusters space-size num-iter");
				System.exit(2);
			}

			int nIter = Integer.parseInt(otherArgs[3]);

			// First iteration
			conf.set("clusters", otherArgs[1]);
			conf.set("N", otherArgs[2]);
			Job job = new Job(conf, "kmeans clustering - " + 1);
			job.setJarByClass(KMeansDriver.class);
			job.setMapperClass(GroupMapper.class);
			//job.setCombinerClass(AverageReducer.class);
			job.setReducerClass(AverageReducer.class);
			job.setOutputKeyClass(Text.class);
			job.setOutputValueClass(Text.class);
			job.setMapOutputKeyClass(Text.class);
			job.setMapOutputValueClass(WritableSIFTKeypoint.class);
			job.setInputFormatClass(SIFTKeypointsInputFormat.class);
			job.setOutputFormatClass(TextOutputFormat.class);
			FileInputFormat.addInputPath(job, new Path(otherArgs[0]));
			FileOutputFormat.setOutputPath(job, new Path("kmeans-step-1"));
			job.waitForCompletion(true);
			
			if(testConvergence(0.1f, "clusters.txt", "kmeans-step-" + (1) + "/part-r-00000", conf))
				return;

			for (int i = 1; i <= nIter - 1; ++i) {

				conf.set("clusters", "kmeans-step-" + (i) + "/part-r-00000");
				conf.set("N", otherArgs[2]);
				job = new Job(conf, "kmeans clustering - " + (i + 1));
				job.setJarByClass(KMeansDriver.class);
				job.setMapperClass(GroupMapper.class);
				//job.setCombinerClass(AverageReducer.class);
				job.setReducerClass(AverageReducer.class);
				job.setOutputKeyClass(Text.class);
				job.setOutputValueClass(Text.class);
				job.setMapOutputKeyClass(Text.class);
				job.setMapOutputValueClass(WritableSIFTKeypoint.class);
				job.setInputFormatClass(SIFTKeypointsInputFormat.class);
				job.setOutputFormatClass(TextOutputFormat.class);
				FileInputFormat.addInputPath(job, new Path(otherArgs[0]));
				FileOutputFormat.setOutputPath(job, new Path("kmeans-step-" + (i + 1) + ""));
				job.waitForCompletion(true);
				
				if(testConvergence(0.01f, "kmeans-step-" + (i) + "/part-r-00000", "kmeans-step-" + (i + 1) + "/part-r-00000", conf))
					return;
			}
		}
		catch (IOException e) {

			e.printStackTrace();
		}
		
	}

	private static boolean testConvergence(float epsilon, String previous, String current, Configuration conf) throws IOException {

		Map<String, float[]> centersPrevious = readClusterCenters(previous, conf);
		Map<String, float[]> centersCurrent = readClusterCenters(current, conf);
		
		float total = 0.0f;
		
		for (String id : centersCurrent.keySet()) {
			
			float[] x = centersPrevious.get(id);
			float[] y = centersCurrent.get(id);
			
			for (int i = 0; i < x.length; ++i) {
				
				float diff = x[i] - y[i];
				
				total += diff * diff;
			}
		}
		
		return Math.sqrt(total) < epsilon;
	}
	
	private static Map<String, float[]> readClusterCenters(String clusters, Configuration conf) throws IOException {
		
		Map<String, float[]> clusterCenters = new java.util.HashMap<String, float[]>();
		int N = Integer.parseInt(conf.get("N"));
		
		FileSystem hdfs = FileSystem.get(conf);
		
		FSDataInputStream fis = hdfs.open(new Path(clusters));
		BufferedReader reader = new BufferedReader(new InputStreamReader(fis));
		String centerStr = null;

		while ((centerStr = reader.readLine()) != null) {
			
			if (centerStr.trim().equals("")) break;

			StringTokenizer centerTok = new StringTokenizer(centerStr);
			
			String centerId = centerTok.nextToken();
			float[] centerData = new float[N];
			int j = 0;

			while (centerTok.hasMoreTokens()) {
				centerData[j] = Float.parseFloat(centerTok.nextToken());
				++j;
			}
			
			clusterCenters.put(centerId, centerData);
		}

		reader.close();
		fis.close();
		
		return clusterCenters;
	}
}

