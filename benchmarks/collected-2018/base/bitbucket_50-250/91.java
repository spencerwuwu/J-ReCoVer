// https://searchcode.com/api/result/122936893/

package src.main.java.filter;

import java.io.IOException;
import java.util.Iterator;
import java.util.regex.Pattern;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

/**
 * <p>
 * ExtractObamaTweets. This program extracts relevant tweets, and
 * takes the following command-line arguments:
 * </p>
 * 
 * <ul>
 * <li>[input-path] input path</li>
 * <li>[output-path] output path</li>
 * <li>[num-reducers] number of reducers</li>
 * </ul>
 * 
 * 
 * Taken and adapted from edu.umd.cloud9.demo.DemoWordCount 
 * 
 * @author Shilpa Shukla
 */
public class ExtractObamaTweets extends Configured {

	// mapper: emits (token, 1) for every word occurrence
	private static class MyMapper extends Mapper<LongWritable, Text, LongWritable, Text> {

		@Override
		public void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
		   
		   String line = value.toString();
		   if(Pattern.compile("obama", Pattern.CASE_INSENSITIVE).matcher(line).find()){
			  context.write(key, value);
		   }
		}
	}

	// reducer: sums up all the counts
	private static class MyReducer extends Reducer<LongWritable, Text, Text, Text> {

			@Override
			public void reduce(LongWritable key, Iterable<Text> values, Context context) 
			throws IOException, InterruptedException {


			context.write(values.iterator().next(), new Text(""));
		}
	}

	public static void main(String[] args) throws Exception {
		if (args.length == 3) {
			String inputPath = args[0];
			String outputPath = args[1];
			int reduceTasks = Integer.parseInt(args[2]);

			Configuration conf = new Configuration();
			Job job = new Job(conf, "ExtractObamaTweets");
			job.setJarByClass(ExtractObamaTweets.class);

			job.setNumReduceTasks(reduceTasks);

			FileInputFormat.setInputPaths(job, new Path(inputPath));
			FileOutputFormat.setOutputPath(job, new Path(outputPath));

			job.setOutputKeyClass(LongWritable.class);
			job.setOutputValueClass(Text.class);

			job.setMapperClass(MyMapper.class);
			job.setReducerClass(MyReducer.class);

			// Delete the output directory if it exists already
			Path outputDir = new Path(outputPath);
			FileSystem.get(conf).delete(outputDir, true);

			long startTime = System.currentTimeMillis();
			job.waitForCompletion(true);
			}
		}
	}

