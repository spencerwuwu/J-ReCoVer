// https://searchcode.com/api/result/54362933/

package kmeans.drivers.bovw.micc.unifi.it;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.Character.Subset;
import java.util.Map;
import java.util.Random;
import java.util.StringTokenizer;

import javax.management.RuntimeErrorException;

import org.apache.hadoop.cli.util.SubstringComparator;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FSDataOutputStream;
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

import types.bovw.micc.unifi.it.WritableSIFTKeypoint;
import formats.bovw.micc.unifi.it.SIFTKeypointsInputFormat;
import formats.bovw.micc.unifi.it.SIFTKeypointsOutputFormat;

public class CopyOfHierarchicalKMeansDriver {

	public static class GroupMapper extends Mapper<Text, WritableSIFTKeypoint, Text, WritableSIFTKeypoint>{
		
		private Text outKey = new Text();
		private WritableSIFTKeypoint outValue = new WritableSIFTKeypoint();
		private Map<String, float[]> centers = null;
		
		@Override
		protected void setup(Context context)
				throws IOException, InterruptedException {
			
			String clusters = context.getConfiguration().get("clusters");
			centers = readClusterCenters(clusters, context.getConfiguration());
			super.setup(context);
		}
		
		public void map(Text key, WritableSIFTKeypoint value, Context context) throws IOException, InterruptedException {
			
			//String clusters = context.getConfiguration().get("clusters");
			int N = Integer.parseInt(context.getConfiguration().get("N"));
			
			float[] pointData = value.getData();

			Configuration conf = context.getConfiguration();

			float minDist = 1000000.0f;
			String minId = "?";
			
			//Map<String, float[]> centers = readClusterCenters(clusters, conf);

			for (String centerId : centers.keySet()) {

				String prefix, centerPrefix;
				
				try {
					prefix = key.toString().substring(0, key.toString().lastIndexOf('.'));
					centerPrefix = centerId.toString().substring(0, centerId.toString().lastIndexOf('.'));
				}
				catch (Exception e) {
					
					throw new RuntimeException(key.toString());
				}
					
				//if (true)
				//	throw new RuntimeException(prefix + " " + centerPrefix + " " + key + " " + centerId);
				
				if (prefix.equals(centerPrefix)) {

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
	
	public static class AverageReducer extends Reducer<Text, WritableSIFTKeypoint, Text, Text> {

		private Text outKey = new Text();
		private Text outValue = new Text();
		
		@Override
		protected void setup(Context context) throws IOException,
				InterruptedException {
			
			super.setup(context);
		}

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

			// TODO farlo gerarchico
			// TODO la scrittura dei primi 2 punti
			// TODO farlo ricorsivo
			
			//runHKMeans("input", "output", conf, otherArgs, nIter);
			runKMeans(conf, otherArgs, nIter);
		}
		catch (IOException e) {

			e.printStackTrace();
		}	
	}
	
	/*
	private static void runHKMeans(String input, String output, Configuration conf, String[] otherArgs,
			int nIter) throws IOException, InterruptedException,
			ClassNotFoundException {
		
		FileSystem hdfs = FileSystem.get(conf);
		
		FSDataInputStream fis = hdfs.open(new Path(input));
		BufferedReader reader = new BufferedReader(new InputStreamReader(fis));
		FSDataOutputStream fos = hdfs.create(new Path(output));
		PrintWriter pw = new PrintWriter(fos);
		
		reader.close();
		fis.close();
		pw.close();
		fos.close();
		
		//clusters.put(key, value);
		//clusters.put(key, value);
		//writeClustersCenter(toWrite, clusters, conf);
		runKMeans(conf, otherArgs, nIter);
	}
	*/

	private static void runKMeans(Configuration conf, String[] otherArgs,
			int nIter) throws IOException, InterruptedException,
			ClassNotFoundException {
		
		// First iteration
		// TODO Splittare in due i mapper-reducer
		//conf.set("clusters", otherArgs[1]);
		conf.set("clusters", "#random(c,2)");
		conf.set("N", otherArgs[2]);
		Job job = new Job(conf, "map clustering - " + 1);
		job.setJarByClass(CopyOfHierarchicalKMeansDriver.class);
		job.setMapperClass(GroupMapper.class);
		//job.setCombinerClass(AverageReducer.class);
		job.setReducerClass(Reducer.class);
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(WritableSIFTKeypoint.class);
		job.setMapOutputKeyClass(Text.class);
		job.setMapOutputValueClass(WritableSIFTKeypoint.class);
		job.setInputFormatClass(SIFTKeypointsInputFormat.class);
		job.setOutputFormatClass(SIFTKeypointsOutputFormat.class);
		FileInputFormat.addInputPath(job, new Path(otherArgs[0]));
		FileOutputFormat.setOutputPath(job, new Path("kmeans-step-1/map"));
		job.waitForCompletion(true);
		
		// TODO Splittare in due i mapper-reducer
		conf.set("clusters", "");
		conf.set("N", otherArgs[2]);
		job = new Job(conf, "reduce clustering - " + 1);
		job.setJarByClass(CopyOfHierarchicalKMeansDriver.class);
		job.setMapperClass(Mapper.class);
		//job.setCombinerClass(AverageReducer.class);
		job.setReducerClass(AverageReducer.class);
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(Text.class);
		job.setMapOutputKeyClass(Text.class);
		job.setMapOutputValueClass(WritableSIFTKeypoint.class);
		job.setInputFormatClass(SIFTKeypointsInputFormat.class);
		job.setOutputFormatClass(TextOutputFormat.class);
		FileInputFormat.addInputPath(job, new Path("kmeans-step-1/map"));
		FileOutputFormat.setOutputPath(job, new Path("kmeans-step-1/reduce"));
		job.waitForCompletion(true);
		
		boolean run = true;
		
		//if(testConvergence(0.1f, otherArgs[1], "kmeans-step-" + (1) + "/reduce/part-r-00000", conf))
		//	run = false;

		int i;
		
		for (i = 1; i <= nIter - 1 && run; ++i) {
			
			conf.set("clusters", "kmeans-step-" + (i) + "/reduce/part-r-00000");
			conf.set("N", otherArgs[2]);
			job = new Job(conf, "map clustering - " + 1);
			job.setJarByClass(CopyOfHierarchicalKMeansDriver.class);
			job.setMapperClass(GroupMapper.class);
			//job.setCombinerClass(AverageReducer.class);
			job.setReducerClass(Reducer.class);
			job.setOutputKeyClass(Text.class);
			job.setOutputValueClass(WritableSIFTKeypoint.class);
			job.setMapOutputKeyClass(Text.class);
			job.setMapOutputValueClass(WritableSIFTKeypoint.class);
			job.setInputFormatClass(SIFTKeypointsInputFormat.class);
			job.setOutputFormatClass(SIFTKeypointsOutputFormat.class);
			FileInputFormat.addInputPath(job, new Path("kmeans-step-" + (i) + "/map"));
			FileOutputFormat.setOutputPath(job, new Path("kmeans-step-" + (i + 1) + "/map"));
			job.waitForCompletion(true);
			
			// TODO Splittare in due i mapper-reducer
			conf.set("clusters", "");
			conf.set("N", otherArgs[2]);
			job = new Job(conf, "reduce clustering - " + 1);
			job.setJarByClass(CopyOfHierarchicalKMeansDriver.class);
			job.setMapperClass(Mapper.class);
			//job.setCombinerClass(AverageReducer.class);
			job.setReducerClass(AverageReducer.class);
			job.setOutputKeyClass(Text.class);
			job.setOutputValueClass(Text.class);
			job.setMapOutputKeyClass(Text.class);
			job.setMapOutputValueClass(WritableSIFTKeypoint.class);
			job.setInputFormatClass(SIFTKeypointsInputFormat.class);
			job.setOutputFormatClass(TextOutputFormat.class);
			FileInputFormat.addInputPath(job, new Path("kmeans-step-" + (i + 1) + "/map"));
			FileOutputFormat.setOutputPath(job, new Path("kmeans-step-" + (i + 1) + "/reduce"));
			job.waitForCompletion(true);
			
			/*
			conf.set("cluscocciniglia rossaters", "kmeans-step-" + (i) + "/reduce/part-r-00000");
			conf.set("N", otherArgs[2]);
			job = new Job(conf, "map clustering - " + (i + 1));
			job.setJarByClass(HierarchicalKMeansDriver.class);
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
			*/
			
			if(testConvergence(0.01f, "kmeans-step-" + (i) + "/reduce/part-r-00000", "kmeans-step-" + (i + 1) + "/reduce/part-r-00000", conf))
				run = false;
		}
		
		conf.set("clusters", "kmeans-step-" + (i) + "/reduce/part-r-00000");
		conf.set("N", otherArgs[2]);
		job = new Job(conf, "map clustering - " + 1);
		job.setJarByClass(CopyOfHierarchicalKMeansDriver.class);
		job.setMapperClass(GroupMapper.class);
		//job.setCombinerClass(AverageReducer.class);
		job.setReducerClass(Reducer.class);
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(WritableSIFTKeypoint.class);
		job.setMapOutputKeyClass(Text.class);
		job.setMapOutputValueClass(WritableSIFTKeypoint.class);
		job.setInputFormatClass(SIFTKeypointsInputFormat.class);
		job.setOutputFormatClass(SIFTKeypointsOutputFormat.class);
		FileInputFormat.addInputPath(job, new Path("kmeans-step-" + (i) + "/map"));
		FileOutputFormat.setOutputPath(job, new Path("clustered"));
		job.waitForCompletion(true);
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
	
	private static Map<String, float[]> readClusterCenters(String clusters,
			Configuration conf) throws IOException {

		int skip = 0;

		Map<String, float[]> clusterCenters = new java.util.HashMap<String, float[]>();
		int N = Integer.parseInt(conf.get("N"));

		if (clusters.startsWith("#random")) {

			Random r = new Random();

			String s1 = clusters.substring(clusters.indexOf('(') + 1);
			String s2 = s1.substring(s1.indexOf(',') + 1);

			String prefix = s1.substring(0, s1.indexOf(','));

			//if (true)
			//	throw new RuntimeException(s1 + " " + s2 + " " + prefix);

			int n = Integer.parseInt(s2.substring(0, s2.indexOf(')')));

			for (int i = 0; i < n; ++i) {

				float[] centerData = new float[N];

				for (int j = 0; j < N; ++j) {

					centerData[j] = r.nextFloat() * 2 - 1;
				}

				clusterCenters.put(prefix + "." + (i + 1), centerData);
			}
		} else {

			FileSystem hdfs = FileSystem.get(conf);

			FSDataInputStream fis = hdfs.open(new Path(clusters));
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					fis));
			String centerStr = null;

			while ((centerStr = reader.readLine()) != null) {

				if (centerStr.trim().equals(""))
					break;

				StringTokenizer centerTok = new StringTokenizer(centerStr);

				String centerId = centerTok.nextToken();
				float[] centerData = new float[N];
				int j = 0;

				while (centerTok.hasMoreTokens()) {

					if (j >= skip) {
						centerData[j - skip] = Float.parseFloat(centerTok
								.nextToken());
					}

					++j;
				}

				clusterCenters.put(centerId, centerData);
			}

			reader.close();
			fis.close();
		}

		return clusterCenters;
	}
	
	private static void writeClustersCenter(Map<String, float[]> toWrite, String clusters, Configuration conf) throws IOException {
		
		FileSystem hdfs = FileSystem.get(conf);
		
		FSDataOutputStream fos = hdfs.create(new Path(clusters));
		PrintWriter pw = new PrintWriter(fos);

		for (String id : toWrite.keySet()) {
			
			pw.print(id + " ");
			
			for (int i = 0; i < toWrite.get(id).length; ++i) {
				
				if (i > 0)
					pw.print(" ");
				
				pw.print(toWrite.get(id)[i]);
			}
			
			pw.println();
		}
	}
}

