// https://searchcode.com/api/result/134047345/

package edu.carleton.mathcs.cs348w13;

import java.io.PipedInputStream;
import java.util.Arrays;

import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

import wmr.core.PageParser;
import wmr.core.Revision;
import wmr.util.LzmaDecompresser;
import wmr.util.Utils;

public class WikiDecompressor extends Configured implements Tool {

	protected static class Map extends
			Mapper<LongWritable, Text, Text, Text> {
		
	    public Text getArticleId(byte[] data, int length) {
	        StringBuffer key = new StringBuffer();
	        for (int i = 0; i < length; i++) {
	            int b = data[i];
	            if (b == '\t') {
	            	return new Text(key.toString());
	            }
	            key.append((char)b);
	        }
	        throw new RuntimeException("No tab separator found");
	    }
	    
		public byte[] getArticleData(byte[] data, int length) {
			for (int i = 0; i < length; i++) {
				if (data[i] == '\t') {
					return Arrays.copyOfRange(data, i + 1, length);
				}
			}
			throw new RuntimeException("No tab separator found");
		}

		protected void map(
				LongWritable key,
				Text value,
				org.apache.hadoop.mapreduce.Mapper<LongWritable, Text, Text, Text>.Context context)
				throws java.io.IOException, InterruptedException {
			byte[] data = value.getBytes();
			int length = value.getLength();
			Text articleId = getArticleId(data, length);
			byte[] escaped = getArticleData(data, length);
			LzmaDecompresser pipe = null;
			try {
				byte[] unescaped = Utils.unescape(escaped, escaped.length);
				pipe = new LzmaDecompresser(unescaped);
				PipedInputStream stream = pipe.decompress();
				byte[] buff = new byte[512];
				int r = stream.read(buff);
				buff = Arrays.copyOfRange(buff, 0, r);
				byte[] reescaped = Utils.escapeWhitespace(buff);
				Text articleText = new Text(reescaped);
				context.write(articleId, articleText);
			} catch (Exception e) {
				throw new RuntimeException(e);
			} finally {
				if (pipe != null) {
					pipe.cleanup();
				}
			}
		}
	}

	public static void main(String[] args) throws Exception {
		ToolRunner.run(new WikiDecompressor(), args);
	}

	@Override
	public int run(String[] args) throws Exception {
		Job job = new Job();
		job.setMapperClass(Map.class);
		// job.setReducerClass(Reduce.class);
		// job.setNumReduceTasks(10);
		job.setMapOutputKeyClass(Text.class);
		job.setMapOutputValueClass(Text.class);
		// job.setInputFormatClass(TextInputFormat.class);
		// job.setOutputFormatClass(TextOutputFormat.class);
		FileInputFormat.setInputPaths(job, new Path(args[0]));
		FileOutputFormat.setOutputPath(job, new Path(args[1]));
		job.waitForCompletion(true);
		return 0;
	}
}

