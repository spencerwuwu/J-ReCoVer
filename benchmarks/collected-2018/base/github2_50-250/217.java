// https://searchcode.com/api/result/70022206/

package edu.sysu.shen.hadoop;

import java.io.IOException;
import java.io.StringReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.SequenceFileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.SequenceFileOutputFormat;
import org.wltea.analyzer.core.IKSegmenter;
import org.wltea.analyzer.core.Lexeme;

/**
 * WordFrequenceInDocument  key:@ value: hadoop1.0.4
 * 
 * @author 
 */
public class WordFrequenceInDocument extends Configured implements Tool {

	// 
	private static final String INPUT_PATH = "/usr/shen/chinesewebkmeans/originaldata";
	// 
	private static final String OUTPUT_PATH = "/usr/shen/chinesewebkmeans/wordcount";

	public static class WordFrequenceInDocMapper extends
			Mapper<LongWritable, Text, Text, IntWritable> {

		// 
		private static final Pattern PATTERN = Pattern
				.compile("[\u4e00-\u9fa5]");
		// 
		private Text word = new Text();
		// 
		private IntWritable singleCount = new IntWritable(1);

		public void map(LongWritable key, Text value, Context context)
				throws IOException, InterruptedException {

			Matcher m = PATTERN.matcher(value.toString());
			// 
			StringBuilder valueBuilder = new StringBuilder();
			// 
			while (m.find()) {
				String matchkey = m.group();
				valueBuilder.append(matchkey);
			}
			String text = valueBuilder.toString();
			// IKanayzer
			StringReader retext = new StringReader(text);
			IKSegmenter ikseg = new IKSegmenter(retext, false);
			Lexeme lex = null;
			while ((lex = ikseg.next()) != null) {
				// 
				this.word.set(lex.getLexemeText() + "@" + key.toString());
				context.write(this.word, this.singleCount);

			}
			valueBuilder.setLength(0);

		}
	}

	public static class WordFrequenceInDocReducer extends
			Reducer<Text, IntWritable, Text, IntWritable> {
		// 
		private IntWritable wordSum = new IntWritable();

		protected void reduce(Text key, Iterable<IntWritable> values,
				Context context) throws IOException, InterruptedException {
			// 
			int sum = 0;
			for (IntWritable val : values) {
				sum += val.get();
			}
			this.wordSum.set(sum);
			context.write(key, this.wordSum);
		}
	}

	public int run(String[] args) throws Exception {

		Configuration conf = getConf();
		Job job = new Job(conf, "Word Frequence In Document");

		job.setJarByClass(WordFrequenceInDocument.class);
		job.setMapperClass(WordFrequenceInDocMapper.class);
		job.setReducerClass(WordFrequenceInDocReducer.class);

		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(IntWritable.class);

		job.setInputFormatClass(SequenceFileInputFormat.class);
		job.setOutputFormatClass(SequenceFileOutputFormat.class);

		FileInputFormat.setInputPaths(job, new Path(INPUT_PATH));
		FileOutputFormat.setOutputPath(job, new Path(OUTPUT_PATH));

		System.exit(job.waitForCompletion(true) ? 0 : 1);
		return 0;
	}

	public static void main(String[] args) throws Exception {
		int res = ToolRunner.run(new Configuration(),
				new WordFrequenceInDocument(), args);
		System.exit(res);
	}
}
