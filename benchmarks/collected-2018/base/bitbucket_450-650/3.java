// https://searchcode.com/api/result/54362912/

package it.unifi.micc.bovw.drivers;

import it.unifi.micc.bovw.types.WritableFloatArray;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.SequenceFile;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.SequenceFileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.SequenceFileOutputFormat;
import org.apache.hadoop.util.GenericOptionsParser;

public class HierarchicalKMeansDriver {

	private static class GroupMapper extends Mapper<Text, WritableFloatArray, Text, WritableFloatArray>{
		
		private Text m_outKey = new Text();
		private WritableFloatArray m_outValue = new WritableFloatArray();
		private Map<String, WritableFloatArray> m_centers = null;
		private Random m_r = new Random();
		private boolean m_split;
		private int m_N;
		private int m_branch;
		
		@Override
		protected void setup(Context context)
				throws IOException, InterruptedException {
			
			String clusters = context.getConfiguration().get("clusters");
			m_centers = readClusterCenters(clusters, context.getConfiguration());
			m_split = Boolean.parseBoolean(context.getConfiguration().get("split"));
			m_N = Integer.parseInt(context.getConfiguration().get("N"));
			
			if (m_split)
				m_branch = Integer.parseInt(context.getConfiguration().get("branch"));
			
			super.setup(context);
		}
		
		public void map(Text key, WritableFloatArray value, Context context) throws IOException, InterruptedException {
			
			float[] pointData = value.getData();
			
			String prefix = null;
			
			try {
				prefix = key.toString().substring(0, key.toString().lastIndexOf('.'));
			}
			catch (RuntimeException ex) {
				throw new RuntimeException(key.toString());
			}
			
			float minDist = 1000000.0f;
			String minId = "?";
			boolean mustSplit = false;

			String s = "";
			
			if (m_centers != null) {
				
				for (String centerId : m_centers.keySet()) {
					
					s += centerId + " " + m_centers.get(centerId) + "|";
					
					String centerPrefix;
					
					//try {
						
						centerPrefix = centerId.toString().substring(0, centerId.toString().lastIndexOf('.'));
					//}
					//catch (Exception e) {
						
					//	throw new RuntimeException(key.toString());
					//}
					
					if (prefix.equals(centerPrefix)) {
	
						float[] centerData = m_centers.get(centerId).getData();
						
						float dist = 0.0f;
		
						for (int k = 0; k < m_N; ++k) {
							float d = pointData[k] - centerData[k];
							dist += d * d;
						}
		
						if (dist < minDist) {
							minDist = dist;
							minId = centerId;
							
							if (m_centers.get(centerId).getFlags().equals("*")) {
								mustSplit = true;
							}
							else {
								mustSplit = false;
							}
						}
					}
				}
			}
			else {
				
				minId = prefix;
				mustSplit = true;
			}
			
			context.setStatus("map[" + m_split + ", " + mustSplit + "]");
			//context.setStatus(s);
			
			if (m_split && mustSplit)
				m_outKey.set(minId + "." + (m_r.nextInt(m_branch) + 1));
			else
				m_outKey.set(minId);
				//m_outKey.set(minId + "&");
			
			m_outValue.setData(value.getData());

			context.write(m_outKey, m_outValue);
		}
	}
	
	private static class AverageReducer extends Reducer<Text, WritableFloatArray, Text, WritableFloatArray> {

		private Text m_outKey = new Text();
		private WritableFloatArray m_outValue = new WritableFloatArray();
		private int m_N;
		private float m_stdDevThreshold;
		
		@Override
		protected void setup(Context context) throws IOException,
				InterruptedException {
			
			m_N = Integer.parseInt(context.getConfiguration().get("N"));
			m_stdDevThreshold = Float.parseFloat(context.getConfiguration().get("stdDevThreshold"));
			super.setup(context);
		}

		public void reduce(Text key, Iterable<WritableFloatArray> values, Context context) throws IOException, InterruptedException {
			
			float[] avgData = new float[m_N];
			int cnt = 0;
			
			List<WritableFloatArray> list = new ArrayList<WritableFloatArray>();

			for (WritableFloatArray val : values) {

				float[] pointData = val.getData();
				
				for (int j = 0; j < m_N; ++j) {
					avgData[j] += pointData[j];
				}

				++cnt;
				list.add(val);
			}

			for (int k = 0; k < m_N; ++k) {
				
				avgData[k] /= cnt;
			}
			
			float[] varData = new float[m_N];
			
			for (WritableFloatArray val : list) {

				float[] pointData = val.getData();
				
				for (int j = 0; j < m_N; ++j) {
					varData[j] += (pointData[j] - avgData[j]) * (pointData[j] - avgData[j]) / cnt;
				}
			}
			
			float avgStdDev = 0.0f;
			
			for (int k = 0; k < m_N; ++k) {
				avgStdDev += Math.sqrt(varData[k]);
			}
			
			avgStdDev /= m_N;
			
			String sym = " < ";
			if (avgStdDev >= m_stdDevThreshold) sym = " >= ";
			context.setStatus("reduce[" + avgStdDev + sym + m_stdDevThreshold + "]");
			
			System.out.println("reduce[" + avgStdDev + sym + m_stdDevThreshold + "]");
			
			//if (true) throw new RuntimeException("reduce[" + avgStdDev + sym + m_stdDevThreshold + "]");
			
			if (avgStdDev >= m_stdDevThreshold)
				m_outValue.setFlags("*");
			else
				m_outValue.setFlags("");
			
			m_outKey.set(key.toString());
			m_outValue.setData(avgData);
			
			context.write(m_outKey, m_outValue);
		}
	}

	public static void main(String[] args) throws Exception {

		try {
			
			Configuration conf = new Configuration();
			String[] otherArgs = new GenericOptionsParser(conf, args).getRemainingArgs();

			if (otherArgs.length != 7) {
				System.err.println("Usage: HierarchicalKMeansDriver points space-size num-levels num-iter-per-level avg-std-dev avg-euclidean-dist branching-factor");
				System.exit(2);
			}

			int nIter = Integer.parseInt(otherArgs[3]);
			int nLevels = Integer.parseInt(otherArgs[2]);
			
			runKMeans(conf, otherArgs, nIter, nLevels);
		}
		catch (IOException e) {

			e.printStackTrace();
		}	
	}
	
	private static void runKMeans(Configuration conf, String[] otherArgs,
			int nIter, int nLevels) throws IOException, InterruptedException,
			ClassNotFoundException {
		
		Job job = null;
		int cnt = 1;
		int lastCnt = -1;
		int lastI = -1;
		
		float avgEuclideanDistance = Float.parseFloat(otherArgs[5]);
		int branch = Integer.parseInt(otherArgs[6]);
		
		while (cnt <= nLevels) {
			
			conf.set("N", otherArgs[1]);
			conf.set("split", "false");
			
			if (cnt == 1) {
				//conf.set("clusters", "#random(c," + branch + ")");
				
				conf.set("split", "true");
				conf.set("branch", "" + branch);
				conf.set("clusters", "#auto()");
				
				job = new Job(conf, "Initial map step");
				FileInputFormat.addInputPath(job, new Path(otherArgs[0]));
			}
			else {
				conf.set("split", "true");
				conf.set("branch", "" + branch);
				conf.set("clusters", "kmeans-step-" + (cnt - 1) + "-" + (lastI) + "/reduce/part-r-00000");
				job = new Job(conf, "Initial map step");
				FileInputFormat.addInputPath(job, new Path("kmeans-step-" + (cnt - 1) + "-" + (lastI) + "/map/part-r-00000"));
			}
			
			job.setJarByClass(HierarchicalKMeansDriver.class);
			job.setMapperClass(GroupMapper.class);
			job.setReducerClass(Reducer.class);
			job.setOutputKeyClass(Text.class);
			job.setOutputValueClass(WritableFloatArray.class);
			job.setMapOutputKeyClass(Text.class);
			job.setMapOutputValueClass(WritableFloatArray.class);
			job.setInputFormatClass(SequenceFileInputFormat.class);
			job.setOutputFormatClass(SequenceFileOutputFormat.class);
			FileOutputFormat.setOutputPath(job, new Path("kmeans-step-" + (cnt) + "-1/map"));
			job.waitForCompletion(true);
			
			conf.set("N", otherArgs[1]);
			conf.set("stdDevThreshold", otherArgs[4]);
			job = new Job(conf, "Initial reduce step");
			job.setJarByClass(HierarchicalKMeansDriver.class);
			job.setMapperClass(Mapper.class);
			job.setReducerClass(AverageReducer.class);
			job.setOutputKeyClass(Text.class);
			job.setOutputValueClass(WritableFloatArray.class);
			job.setMapOutputKeyClass(Text.class);
			job.setMapOutputValueClass(WritableFloatArray.class);
			job.setInputFormatClass(SequenceFileInputFormat.class);
			job.setOutputFormatClass(SequenceFileOutputFormat.class);
			FileInputFormat.addInputPath(job, new Path("kmeans-step-" + (cnt) + "-1/map"));
			FileOutputFormat.setOutputPath(job, new Path("kmeans-step-" + (cnt) + "-1/reduce"));
			job.waitForCompletion(true);
			
			boolean run = true;
	
			int i;
			
			for (i = 1; i <= nIter - 1 && run; ++i) {
				
				conf.set("split", "false");
				conf.set("clusters", "kmeans-step-" + (cnt) + "-" + (i) + "/reduce/part-r-00000");
				conf.set("N", otherArgs[1]);
				job = new Job(conf, "Intermediate map step");
				job.setJarByClass(HierarchicalKMeansDriver.class);
				job.setMapperClass(GroupMapper.class);
				job.setReducerClass(Reducer.class);
				job.setOutputKeyClass(Text.class);
				job.setOutputValueClass(WritableFloatArray.class);
				job.setMapOutputKeyClass(Text.class);
				job.setMapOutputValueClass(WritableFloatArray.class);
				job.setInputFormatClass(SequenceFileInputFormat.class);
				job.setOutputFormatClass(SequenceFileOutputFormat.class);
				FileInputFormat.addInputPath(job, new Path("kmeans-step-" + (cnt) + "-" + (i) + "/map"));
				FileOutputFormat.setOutputPath(job, new Path("kmeans-step-" + (cnt) + "-" + (i + 1) + "/map"));
				job.waitForCompletion(true);
				
				conf.set("N", otherArgs[1]);
				conf.set("stdDevThreshold", otherArgs[4]);
				job = new Job(conf, "Intermediate reduce step");
				job.setJarByClass(HierarchicalKMeansDriver.class);
				job.setMapperClass(Mapper.class);
				job.setReducerClass(AverageReducer.class);
				job.setOutputKeyClass(Text.class);
				job.setOutputValueClass(WritableFloatArray.class);
				job.setMapOutputKeyClass(Text.class);
				job.setMapOutputValueClass(WritableFloatArray.class);
				job.setInputFormatClass(SequenceFileInputFormat.class);
				job.setOutputFormatClass(SequenceFileOutputFormat.class);
				FileInputFormat.addInputPath(job, new Path("kmeans-step-" + (cnt) + "-" + (i + 1) + "/map"));
				FileOutputFormat.setOutputPath(job, new Path("kmeans-step-" + (cnt) + "-" + (i + 1) + "/reduce"));
				job.waitForCompletion(true);
				
				if(testConvergence(avgEuclideanDistance, "kmeans-step-" + (cnt) + "-" + (i) + "/reduce/part-r-00000", "kmeans-step-" + (cnt) + "-" + (i + 1) + "/reduce/part-r-00000", conf))
					run = false;
				
				lastI = i;
			}
			
			lastCnt = cnt;
			
			cnt++;
		}
		
		conf.set("split", "false");
		conf.set("clusters", "kmeans-step-" + (lastCnt) + "-" + (lastI) + "/reduce/part-r-00000");
		conf.set("N", otherArgs[1]);
		job = new Job(conf, "Final map step");
		job.setJarByClass(HierarchicalKMeansDriver.class);
		job.setMapperClass(GroupMapper.class);
		job.setReducerClass(Reducer.class);
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(WritableFloatArray.class);
		job.setMapOutputKeyClass(Text.class);
		job.setMapOutputValueClass(WritableFloatArray.class);
		job.setInputFormatClass(SequenceFileInputFormat.class);
		job.setOutputFormatClass(SequenceFileOutputFormat.class);
		FileInputFormat.addInputPath(job, new Path("kmeans-step-" + (lastCnt) + "-" + (lastI) + "/map"));
		FileOutputFormat.setOutputPath(job, new Path("clustered"));
		job.waitForCompletion(true);
	}

	private static boolean testConvergence(float avgEuclideanDistance, String previous, String current, Configuration conf) throws IOException {

		Map<String, WritableFloatArray> centersPrevious = readClusterCenters(previous, conf);
		Map<String, WritableFloatArray> centersCurrent = readClusterCenters(current, conf);
		
		float total = 0.0f;
		
		// TODO Sistemare questa parte
		Set<String> common = new HashSet<String>(centersCurrent.keySet());
		common.retainAll(centersPrevious.keySet());
		
		//System.out.println("" + centersCurrent.size() + " " + centersPrevious.size());
		System.out.println("" + (total / common.size()) + " < " + avgEuclideanDistance);
		
		for (String centerId : common) {
			
			float[] x = centersPrevious.get(centerId).getData();
			float[] y = centersCurrent.get(centerId).getData();
			
			float t = 0.0f;
			
			for (int i = 0; i < x.length; ++i) {
				
				float diff = x[i] - y[i];
				
				t += diff * diff;
			}
			
			total += Math.sqrt(t);
		}
		
		return (total / common.size()) < avgEuclideanDistance;
		//return false;
	}
	
	private static Map<String, WritableFloatArray> readClusterCenters(String clusters,
			Configuration conf) throws IOException {
		
		Map<String, WritableFloatArray> clusterCenters = new java.util.HashMap<String, WritableFloatArray>();
		int N = Integer.parseInt(conf.get("N"));

		if (clusters.startsWith("#random")) {

			Random random = new Random();

			String s1 = clusters.substring(clusters.indexOf('(') + 1);
			String s2 = s1.substring(s1.indexOf(',') + 1);

			String prefix = s1.substring(0, s1.indexOf(','));
			int n = Integer.parseInt(s2.substring(0, s2.indexOf(')')));

			for (int i = 0; i < n; ++i) {

				float[] centerData = new float[N];

				for (int j = 0; j < N; ++j) {

					centerData[j] = 2.0f * random.nextFloat() - 1.0f;
				}

				WritableFloatArray c = new WritableFloatArray();
				c.setData(centerData);
				clusterCenters.put(prefix + "." + (i + 1), c);
			}
		} else if (clusters.startsWith("#auto")) {
		
			return null;
			
		} else {

			FileSystem hdfs = FileSystem.get(conf);

			SequenceFile.Reader reader = new SequenceFile.Reader(hdfs, new Path(clusters), conf);
			
			//int cnt = 0;
			//String s = "";
			
			boolean run = true;
			
			while (run) {

				Text key = new Text();
				WritableFloatArray value = new WritableFloatArray();
				run = reader.next(key, value);
				
				if (run) {
					
					clusterCenters.put(key.toString(), value);
				}
				//++cnt;
				//s += key + " " + value + " ";
			}

			reader.close();
			
			//throw new RuntimeException(s);
		}

		return clusterCenters;
	}
}

