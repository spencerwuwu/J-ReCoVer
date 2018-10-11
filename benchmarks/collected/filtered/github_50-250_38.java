// https://searchcode.com/api/result/99975950/

import java.io.*;
import java.util.*;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.conf.*;
import org.apache.hadoop.io.*;
import org.apache.hadoop.mapred.*;
import org.apache.hadoop.util.*;

public class Fox_Martin_exercise2 extends Configured implements Tool {
	public static class Map extends MapReduceBase implements Mapper<LongWritable, Text, Text, DoubleWritable> {
		private DoubleWritable col4 = new DoubleWritable();
		private Text combo = new Text();

		public void configure(JobConf job) {

		}

		public void map(LongWritable key, Text inputLine, OutputCollector<Text, DoubleWritable> output, Reporter reporter) throws IOException {
			String line = inputLine.toString();
		    StringTokenizer tokenizer = new StringTokenizer(line, ",");
		    while (tokenizer.hasMoreTokens()) {
		    	for(int i = 1; i < 4; i++)	
		    		tokenizer.nextToken();
		    	col4.set(Double.parseDouble(tokenizer.nextToken()));
		    	for(int i = 5; i < 30; i++)	
		    		tokenizer.nextToken();
				combo.set(tokenizer.nextToken()+", "+tokenizer.nextToken()+", "+tokenizer.nextToken()+", "+tokenizer.nextToken());
				
				String lastColumn = "";
				while (tokenizer.hasMoreTokens()) {
					lastColumn = tokenizer.nextToken();
				}
				
				if(lastColumn.equals("false"))
					output.collect(combo, col4);
			}
		}
	}	

	public static class Reduce extends MapReduceBase implements Reducer<Text, DoubleWritable, Text, DoubleWritable> {
		public void reduce(Text key, Iterator<DoubleWritable> values, OutputCollector<Text, DoubleWritable> output, Reporter reporter) throws IOException {
			double avgVal = 0;
			int count = 0;
			double sum = 0;
			while(values.hasNext()){
				sum += values.next().get();
				count++;
			}
			avgVal = sum/count;
			output.collect(key, new DoubleWritable(avgVal));
		}
	}

	public int run(String[] args) throws Exception{
		JobConf conf = new JobConf(getConf(), Fox_Martin_exercise2.class);
		conf.setJobName("ibm");

		conf.setOutputKeyClass(Text.class);
		conf.setOutputValueClass(DoubleWritable.class);

		conf.setMapperClass(Map.class);
		//conf.setCombinerClass(Reduce.class);
		conf.setReducerClass(Reduce.class);

		conf.setInputFormat(TextInputFormat.class);
		conf.setOutputFormat(TextOutputFormat.class);

		FileInputFormat.setInputPaths(conf, new Path(args[0]));
		FileOutputFormat.setOutputPath(conf, new Path(args[1]));

		JobClient.runJob(conf);
		return 0;
	}	

	public static void main(String[] args) throws Exception{
		int res = ToolRunner.run(new Configuration(), new Fox_Martin_exercise2(), args);
		System.exit(res);
	}
} 
