// https://searchcode.com/api/result/65663884/

/*
 * Cloud9: A MapReduce Library for Hadoop
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you
 * may not use this file except in compliance with the License. You may
 * obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package edu.umd.cloud9.example.bigram;

import java.io.IOException;
import java.util.Iterator;
import java.util.StringTokenizer;

import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.FloatWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Partitioner;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.SequenceFileOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.apache.log4j.Logger;

import edu.umd.cloud9.io.Schema;
import edu.umd.cloud9.io.Tuple;

public class BigramRelativeFrequencyTuple extends Configured implements Tool {

	private static final Logger LOG = Logger.getLogger(BigramRelativeFrequencyTuple.class);

	private static final Schema SCHEMA = new Schema();

	// Define the schema statically.
	static {
		SCHEMA.addField("Left", String.class, "");
		SCHEMA.addField("Right", String.class, "");
	}

	// Mapper: emits (token, 1) for every bigram occurrence.
	protected static class MyMapper extends	Mapper<LongWritable, Text, Tuple, FloatWritable> {
		// Reuse objects to save overhead of object creation.
		private static final FloatWritable one = new FloatWritable(1);
		private static final Tuple tuple = SCHEMA.instantiate();

		@Override
		public void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
			String line = value.toString();

			String prev = null;
			StringTokenizer itr = new StringTokenizer(line);
			while (itr.hasMoreTokens()) {
				String cur = itr.nextToken();

				// Emit only if we have an actual bigram.
				if (prev != null) {

					// Simple way to truncate tokens that are too long.
					if (cur.length() > 100) {
						cur = cur.substring(0, 100);
					}

					if (prev.length() > 100) {
						prev = prev.substring(0, 100);
					}

					tuple.set("Left", prev);
					tuple.set("Right", cur);
					context.write(tuple, one);

					tuple.set("Left", prev);
					tuple.setSymbol("Right", "*");
					context.write(tuple, one);
				}
				prev = cur;
			}
		}
	}

	protected static class MyCombiner extends Reducer<Tuple, FloatWritable, Tuple, FloatWritable> {
		private final static FloatWritable sumWritable = new FloatWritable();

		@Override
		public void reduce(Tuple key, Iterable<FloatWritable> values, Context context) throws IOException, InterruptedException {
			int sum = 0;
			Iterator<FloatWritable> iter = values.iterator();
			while (iter.hasNext()) {
				sum += iter.next().get();
			}
			sumWritable.set(sum);
			context.write(key, sumWritable);
		}
	}

	protected static class MyReducer extends Reducer<Tuple, FloatWritable, Tuple, FloatWritable> {
		private static final FloatWritable value = new FloatWritable();
		private float marginal = 0.0f;

		@Override
		public void reduce(Tuple key, Iterable<FloatWritable> values, Context context) throws IOException, InterruptedException {
			float sum = 0.0f;
			Iterator<FloatWritable> iter = values.iterator();
			while (iter.hasNext()) {
				sum += iter.next().get();
			}

			if (key.containsSymbol("Right") && key.getSymbol("Right").equals("*")) {
				value.set(sum);
				context.write(key, value);
				marginal = sum;
			} else {
				value.set(sum / marginal);
				context.write(key, value);
			}
		}
	}

	protected static class MyPartitioner extends Partitioner<Tuple, FloatWritable> {
		@Override
		public int getPartition(Tuple key, FloatWritable value, int numReduceTasks) {
			return (key.get("Left").hashCode() & Integer.MAX_VALUE) % numReduceTasks;
		}
	}

	private BigramRelativeFrequencyTuple() {}

	private static int printUsage() {
		System.out.println("usage: [input-path] [output-path] [num-reducers]");
		ToolRunner.printGenericCommandUsage(System.out);
		return -1;
	}

	/**
	 * Runs this tool.
	 */
	public int run(String[] args) throws Exception {
		if (args.length != 3) {
			printUsage();
			return -1;
		}

		String inputPath = args[0];
		String outputPath = args[1];
		int reduceTasks = Integer.parseInt(args[2]);

		LOG.info("Tool name: BigramRelativeFrequencyTuple");
		LOG.info(" - input path: " + inputPath);
		LOG.info(" - output path: " + outputPath);
		LOG.info(" - num reducers: " + reduceTasks);

		Job job = new Job(getConf(), "BigramRelativeFrequencyTuple");
		job.setJarByClass(BigramRelativeFrequencyTuple.class);

		job.setNumReduceTasks(reduceTasks);

		FileInputFormat.setInputPaths(job, new Path(inputPath));
		FileOutputFormat.setOutputPath(job, new Path(outputPath));

		job.setMapOutputKeyClass(Tuple.class);
		job.setMapOutputValueClass(FloatWritable.class);
		job.setOutputKeyClass(Tuple.class);
		job.setOutputValueClass(FloatWritable.class);
		job.setOutputFormatClass(SequenceFileOutputFormat.class);

		job.setMapperClass(MyMapper.class);
		job.setCombinerClass(MyCombiner.class);
		job.setReducerClass(MyReducer.class);
		job.setPartitionerClass(MyPartitioner.class);

		// Delete the output directory if it exists already
		Path outputDir = new Path(outputPath);
		FileSystem.get(getConf()).delete(outputDir, true);

		long startTime = System.currentTimeMillis();
		job.waitForCompletion(true);
		System.out.println("Job Finished in " + (System.currentTimeMillis() - startTime) / 1000.0 + " seconds");

		return 0;
	}

	/**
	 * Dispatches command-line arguments to the tool via the
	 * <code>ToolRunner</code>.
	 */
	public static void main(String[] args) throws Exception {
		int res = ToolRunner.run(new BigramRelativeFrequencyTuple(), args);
		System.exit(res);
	}
}

