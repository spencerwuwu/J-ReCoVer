// https://searchcode.com/api/result/134046358/

/**
 * MapReduce job that pipes input to output as MapReduce-created key-val pairs
 * (c) 2012 Jeannie Albrecht
 */

import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.FileSplit;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

public class InvertedIndex extends Configured implements Tool {
	public static void main(String[] args) throws Exception {
		int res = ToolRunner.run(new Configuration(), new InvertedIndex(), args);
		System.exit(res);
	}

	/* (non-Javadoc)
	 * @see org.apache.hadoop.util.Tool#run(java.lang.String[])
	 */
	@Override
	public int run(String[] args) throws Exception {
		if (args.length < 2) {
			System.err.println("Error: Wrong number of paramaters");
			System.err.println("trivial ");
			System.exit(1);
		}

		Configuration conf = getConf();
		Job job = new Job(conf, "trivial");

		job.setJarByClass(InvertedIndex.class);
		job.setMapperClass(InvertedIndex.InvertedIndexMapper.class);
		job.setReducerClass(InvertedIndex.InvertedIndexReducer.class);
		job.setMapOutputKeyClass(Text.class);

		job.setInputFormatClass(TextInputFormat.class);
		job.setOutputFormatClass(TextOutputFormat.class);
		FileInputFormat.addInputPath(job, new Path(args[0]));
		FileOutputFormat.setOutputPath(job, new Path(args[1]));

		System.out.println(job.getInputFormatClass());

		return job.waitForCompletion(true) ? 0 : 1;
	}

	/**
	 * map: (LongWritable, Text) --> (LongWritable, Text)
	 * 
	 * NOTE: Keys must implement WritableComparable; values must implement
	 * Writable.
	 */
	public static class InvertedIndexMapper extends
			Mapper<LongWritable, Text, Text, Text> {
		private Text myword = new Text();
		private Text myfile = new Text();
		
		/* (non-Javadoc)
		 * @see org.apache.hadoop.mapreduce.Mapper#map(KEYIN, VALUEIN, org.apache.hadoop.mapreduce.Mapper.Context)
		 */
		@Override
		public void map(LongWritable key, Text val, Context context)
				throws IOException, InterruptedException {			
			FileSplit split = (FileSplit) context.getInputSplit();
			String filename = split.getPath().getName();
			myfile.set(filename);
			
			String line = val.toString();
			StringTokenizer tokenizer = new StringTokenizer(line);
			
			while (tokenizer.hasMoreTokens()) {
				myword.set(tokenizer.nextToken());
				context.write(myword, myfile);
			}
		}
	}

	/**
	 * reduce: (Text, Text[]) --> (Text, Text)[]
	 */
	public static class InvertedIndexReducer extends
			Reducer<Text, Text, Text, Text> {		
		/* (non-Javadoc)
		 * @see org.apache.hadoop.mapreduce.Reducer#reduce(KEYIN, java.lang.Iterable, org.apache.hadoop.mapreduce.Reducer.Context)
		 */
		@Override
		public void reduce(Text key, Iterable<Text> value,
				Context context) throws IOException, InterruptedException {
			Set<String> uniqueFiles = new HashSet<String>();
			for (Text val : value) {
				uniqueFiles.add(val.toString());
			}
			
			Iterator<String> it = uniqueFiles.iterator();
			StringBuffer mytext = new StringBuffer(it.next());
			while (it.hasNext()) {
				mytext.append(", ");
				mytext.append(it.next());
			}
			context.write(key, new Text(mytext.toString()));
		}
	}
}
