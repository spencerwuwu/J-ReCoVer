// https://searchcode.com/api/result/99975914/

import java.io.*;
import java.util.*;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.conf.*;
import org.apache.hadoop.io.*;
import org.apache.hadoop.mapred.*;
import org.apache.hadoop.util.*;
import org.apache.hadoop.mapred.lib.*;

public class Li_Lullo_Martin_exercise2 extends Configured implements Tool {
	public static class Map1 extends MapReduceBase implements Mapper<LongWritable, Text, Text, DoubleWritable> {
		private DoubleWritable numVols = new DoubleWritable();
		private final Text dummyKey = new Text(" ");

		public void configure(JobConf job) {

		}

		public void map(LongWritable key, Text inputLine, OutputCollector<Text, DoubleWritable> output, Reporter reporter) throws IOException {
			String line = inputLine.toString();
			StringTokenizer tokenizer = new StringTokenizer(line);
	    	tokenizer.nextToken(); //word
	    	tokenizer.nextToken(); //year
	    	tokenizer.nextToken(); //# occurrences - don't need
	    	numVols.set(Double.parseDouble(tokenizer.nextToken()));
			output.collect(dummyKey, numVols);
		}
	}	

	public static class Map2 extends MapReduceBase implements Mapper<LongWritable, Text, Text, DoubleWritable> {
		private DoubleWritable numVols = new DoubleWritable();
		private final Text dummyKey = new Text(" ");

		public void configure(JobConf job) {

		}

		public void map(LongWritable key, Text inputLine, OutputCollector<Text, DoubleWritable> output, Reporter reporter) throws IOException {
			String line = inputLine.toString();
			StringTokenizer tokenizer = new StringTokenizer(line);
	    	tokenizer.nextToken(); //word1
	    	tokenizer.nextToken(); //word2
	    	tokenizer.nextToken(); //year
	    	tokenizer.nextToken(); //# occurrences - don't need
	    	numVols.set(Double.parseDouble(tokenizer.nextToken()));
			output.collect(dummyKey, numVols);
		}
	}	

	public static class Reduce extends MapReduceBase implements Reducer<Text, DoubleWritable, Text, DoubleWritable> {
		public void reduce(Text key, Iterator<DoubleWritable> values, OutputCollector<Text, DoubleWritable> output, Reporter reporter) throws IOException {
			double stdDev = 0;
			double sumSqr = 0;
			double count = 0;
			double mean = 0;
			double sum = 0;
			while(values.hasNext()){
				double value = values.next().get();
				sumSqr += value*value;
				sum += value;
				count++;
			}
			mean = sum/count;
			stdDev = Math.sqrt((sumSqr-count*mean*mean)/count);
			output.collect(key, new DoubleWritable(stdDev));
		}
	}


	public int run(String[] args) throws Exception{
		JobConf conf = new JobConf(getConf(), Li_Lullo_Martin_exercise2.class);
		conf.setJobName("hw2ex2");

		conf.setOutputKeyClass(Text.class);
		conf.setOutputValueClass(DoubleWritable.class);

		conf.setMapperClass(Map1.class);
		conf.setMapperClass(Map2.class);
		// conf.setCombinerClass(Reduce.class);
		conf.setReducerClass(Reduce.class);

		conf.setInputFormat(TextInputFormat.class);
		conf.setOutputFormat(TextOutputFormat.class);

		MultipleInputs.addInputPath(conf,new Path(args[0]+"/googlebooks-eng-all-1gram-20120701-6"),TextInputFormat.class,Map1.class);
		MultipleInputs.addInputPath(conf,new Path(args[0]+"/googlebooks-eng-all-2gram-20120701-6"),TextInputFormat.class,Map2.class);
		FileOutputFormat.setOutputPath(conf, new Path(args[1]));

		JobClient.runJob(conf);
		return 0;
	}	

	public static void main(String[] args) throws Exception{
		int res = ToolRunner.run(new Configuration(), new Li_Lullo_Martin_exercise2(), args);
		System.exit(res);
	}
} 
