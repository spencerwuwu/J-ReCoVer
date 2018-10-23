// https://searchcode.com/api/result/75862781/

package oldapi;

import java.io.IOException;
import java.util.Iterator;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
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

import safe.SafeTextInputFormat;

public class WordCountOld {

	public static class WordMapper extends MapReduceBase implements
			Mapper<LongWritable, Text, Text, IntWritable> {
		public void map(LongWritable key, Text value,
				OutputCollector<Text, IntWritable> output, Reporter reporter)
				throws IOException {

			output.collect(value, new IntWritable(1));

		}
	}

	public static class WordReducer extends MapReduceBase implements
			Reducer<Text, IntWritable, Text, IntWritable> {
		public void reduce(Text key, Iterator<IntWritable> values,
				OutputCollector<Text, IntWritable> output, Reporter reporter)
				throws IOException {
			int sum = 0;
			while (values.hasNext()) {
				sum += values.next().get();
			}
			output.collect(key, new IntWritable(sum));
		}

	}

	public static void main(String[] args) throws Exception {

		Path inputPath = new Path(args[0]);
		Path outputPath = new Path(args[1]);
		
		System.out.println("1");
		Configuration config = new Configuration();
        FileSystem hdfs = FileSystem.get(outputPath.toUri(),config);
        hdfs.delete(outputPath, true);
        hdfs.close();
		
		JobConf conf = new JobConf(WordCountOld.class);
		conf.setJarByClass(WordCountOld.class);
		conf.setJobName("word count");
		
		conf.setInputFormat(SafeTextInputFormat.class);
        
        System.out.println("2");
		FileInputFormat.setInputPaths(conf, inputPath);
		FileOutputFormat.setOutputPath(conf, outputPath);
		
		conf.setMapperClass(WordMapper.class);
		conf.setReducerClass(WordReducer.class);
		conf.setMapOutputKeyClass(Text.class);
		conf.setMapOutputValueClass(IntWritable.class);
		conf.setOutputKeyClass(Text.class);
		conf.setOutputValueClass(IntWritable.class);
		conf.setNumReduceTasks(0);
		JobClient.runJob(conf);
	}
	
}

