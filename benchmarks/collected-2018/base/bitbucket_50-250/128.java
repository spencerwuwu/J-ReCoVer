// https://searchcode.com/api/result/124699288/

/**
 * 
 */
package org.jaggu.hadoop.examples;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.FileInputFormat;
import org.apache.hadoop.mapred.FileOutputFormat;
import org.apache.hadoop.mapred.JobClient;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.MapReduceBase;
import org.apache.hadoop.mapred.Mapper;
import org.apache.hadoop.mapred.OutputCollector;
import org.apache.hadoop.mapred.Reducer;
import org.apache.hadoop.mapred.Reporter;
import org.apache.hadoop.mapred.TextInputFormat;
import org.apache.hadoop.mapred.TextOutputFormat;

/**
 * @author Jaganadh G
 * 
 */
public class BigramFrequency {

	// Mapper

	public static class BigramMapper extends MapReduceBase implements
			Mapper<LongWritable, Text, Text, IntWritable> {
		private final static IntWritable count = new IntWritable(1);
		private Text bigram = new Text();

		public void map(LongWritable key, Text value,
				OutputCollector<Text, IntWritable> output, Reporter reporter)
				throws IOException {
			String strText = value.toString().replaceAll("\\p{Punct}+", " ");
			StringTokenizer tokenizer = new StringTokenizer(strText);
			List<String> tokens = new ArrayList<String>();
			while (tokenizer.hasMoreTokens()) {
				String currentToken = tokenizer.nextToken().toLowerCase()
						.replaceAll("\\p{Punct}+", " ");
				if (currentToken.length() > 1) {
					tokens.add(currentToken);
				}

			}

			for (int i = 0; i < tokens.size() - 1; i++) {
				bigram.set(tokens.get(i) + " " + tokens.get(i + 1));
				output.collect(bigram, count);
			}
		}

	}

	// Reducer
	public static class BigramReducer extends MapReduceBase implements
			Reducer<Text, IntWritable, Text, IntWritable> {
		public void reduce(Text key, Iterator<IntWritable> values,
				OutputCollector<Text, IntWritable> output, Reporter reporter)
				throws IOException {
			int intSumBigram = 0;
			while (values.hasNext()) {
				intSumBigram += values.next().get();
			}
			output.collect(key, new IntWritable(intSumBigram));
		}
	}

	public static void main(String[] args) throws IOException {
		JobConf bigramJob = new JobConf(BigramFrequency.class);
		bigramJob.setJobName("bigram_count");

		bigramJob.setOutputKeyClass(Text.class);
		bigramJob.setOutputValueClass(IntWritable.class);

		bigramJob.setMapperClass(BigramMapper.class);
		bigramJob.setCombinerClass(BigramReducer.class);
		bigramJob.setReducerClass(BigramReducer.class);

		bigramJob.setInputFormat(TextInputFormat.class);
		bigramJob.setOutputFormat(TextOutputFormat.class);

		FileInputFormat.setInputPaths(bigramJob, new Path(args[0]));
		FileOutputFormat.setOutputPath(bigramJob, new Path(args[1]));

		JobClient.runJob(bigramJob);

	}

}

