// https://searchcode.com/api/result/134047346/

package edu.carleton.mathcs.cs348w13;

import java.io.IOException;
import java.util.Arrays;

import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

import wmr.core.PageParser;
import wmr.core.Revision;
import wmr.core.User;
import wmr.util.LzmaDecompresser;
import wmr.util.Utils;

/**
 * This class counts the number of revisions by each Wikipedia contributor.
 */
public class RevisionCounter extends Configured implements Tool {

	public static class Map extends
			Mapper<LongWritable, Text, Text, IntWritable> {
		private IntWritable one = new IntWritable(1);
		private Text word = new Text();

		/**
		 * Get the compressed article from a line of input. A line of input
		 * contains an article ID, a tab character, and a compressed article.
		 * 
		 * @param data
		 *            line of input
		 * @param length
		 *            length of line (may be < data.length)
		 * @return byte array containing just the compressed article.
		 */
		private byte[] getValue(byte[] data, int length) {
			for (int i = 0; i < length; i++) {
				if (data[i] == '\t') {
					return Arrays.copyOfRange(data, i + 1, length);
				}
			}
			throw new RuntimeException("No tab separator found");
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.apache.hadoop.mapreduce.Mapper#map(KEYIN, VALUEIN,
		 * org.apache.hadoop.mapreduce.Mapper.Context)
		 */
		@Override
		protected void map(
				LongWritable key,
				Text value,
				org.apache.hadoop.mapreduce.Mapper<LongWritable, Text, Text, IntWritable>.Context context)
				throws java.io.IOException, InterruptedException {
			// Get the article
			byte[] data = value.getBytes();
			int length = value.getLength();
			byte[] escaped = getValue(data, length);

			LzmaDecompresser pipe = null;
			try {
				// Unescape tab/newline characters
				byte[] unescaped = Utils.unescape(escaped, escaped.length);

				// Decompress the article and parse it
				pipe = new LzmaDecompresser(unescaped);
				PageParser parser = new PageParser(pipe.decompress());
				System.err.println("Article name: "
						+ parser.getArticle().getName());

				// For each non-anonymous revision, emit (username, 1)
				while (parser.hasNextRevision()) {
					Revision r = parser.getNextRevision();
					User contributor = r.getContributor();
					if (!contributor.isAnonymous()) {
						String name = contributor.getName();
						word.set(name);
						context.write(word, one);
					}
				}

			} catch (Exception e) {
				throw new RuntimeException(e);
			} finally {
				if (pipe != null) {
					pipe.cleanup();
				}
			}
		}
	}

	public static class Reduce extends
			Reducer<Text, IntWritable, Text, IntWritable> {
		/*
		 * (non-Javadoc)
		 * 
		 * @see org.apache.hadoop.mapreduce.Reducer#reduce(KEYIN,
		 * java.lang.Iterable, org.apache.hadoop.mapreduce.Reducer.Context)
		 */
		@Override
		protected void reduce(Text key, Iterable<IntWritable> values,
				Reducer<Text, IntWritable, Text, IntWritable>.Context context)
				throws IOException, InterruptedException {
			// For each contributor, sum the number of contrbutions and emit
			// (username, sum)
			int sum = 0;
			for (IntWritable value : values) {
				sum += value.get();
			}
			context.write(key, new IntWritable(sum));
		}
	}

	public static void main(String[] args) throws Exception {
		ToolRunner.run(new RevisionCounter(), args);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.hadoop.util.Tool#run(java.lang.String[])
	 */
	@Override
	public int run(String[] args) throws Exception {
		Job job = new Job();
		job.setJarByClass(RevisionCounter.class);
		job.setMapperClass(Map.class);
		job.setReducerClass(Reduce.class);
		job.setNumReduceTasks(10);
		job.setMapOutputKeyClass(Text.class);
		job.setMapOutputValueClass(IntWritable.class);
		// job.setInputFormatClass(TextInputFormat.class);
		// job.setOutputFormatClass(TextOutputFormat.class);
		FileInputFormat.setInputPaths(job, new Path(args[0]));
		FileOutputFormat.setOutputPath(job, new Path(args[1]));
		job.waitForCompletion(true);
		return 0;
	}
}

