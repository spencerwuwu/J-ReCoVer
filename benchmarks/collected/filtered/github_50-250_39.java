// https://searchcode.com/api/result/99975954/

import java.io.*;
import java.util.*;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.conf.*;
import org.apache.hadoop.io.*;
import org.apache.hadoop.mapred.*;
import org.apache.hadoop.util.*;

public class Fox_Martin_exercise1 extends Configured implements Tool {
	public static class Map extends MapReduceBase implements Mapper<LongWritable, Text, Text, IntWritable> {
		private IntWritable temp = new IntWritable();
		private Text year = new Text();

		public void configure(JobConf job) {

		}

		public void map(LongWritable key, Text inputLine, OutputCollector<Text, IntWritable> output, Reporter reporter) throws IOException {
			String line = inputLine.toString();

			String sign = line.substring(87,88);
			int mult = 0;
			if (sign.equals("-"))
				mult = -1;
			if (sign.equals("+"))
				mult = 1;
			int inputTemp = mult*Integer.parseInt(line.substring(88,92));
			int tempQuality = Integer.parseInt(line.substring(92,93));

			if(inputTemp!=9999 && (tempQuality==0 || tempQuality==1 || tempQuality==4 || tempQuality==5 || tempQuality==9)) {
				String inputYear = line.substring(15,19);
				year.set(inputYear);
				temp.set(inputTemp);
				output.collect(year, temp);
			}
		}
	}	

	public static class Reduce extends MapReduceBase implements Reducer<Text, IntWritable, Text, IntWritable> {
		public void reduce(Text key, Iterator<IntWritable> values, OutputCollector<Text, IntWritable> output, Reporter reporter) throws IOException {
			int maxTemp = 0;
			while(values.hasNext()){
				int nextTemp = values.next().get();
				if (nextTemp > maxTemp)
					maxTemp = nextTemp;
			}
			output.collect(key, new IntWritable(maxTemp));
		}
	}

	public int run(String[] args) throws Exception{
		JobConf conf = new JobConf(getConf(), Fox_Martin_exercise1.class);
		conf.setJobName("maxtemp");

		conf.setOutputKeyClass(Text.class);
		conf.setOutputValueClass(IntWritable.class);

		conf.setMapperClass(Map.class);
		conf.setCombinerClass(Reduce.class);
		conf.setReducerClass(Reduce.class);

		conf.setInputFormat(TextInputFormat.class);
		conf.setOutputFormat(TextOutputFormat.class);

		FileInputFormat.setInputPaths(conf, new Path(args[0]));
		FileOutputFormat.setOutputPath(conf, new Path(args[1]));

		JobClient.runJob(conf);
		return 0;
	}	

	public static void main(String[] args) throws Exception{
		int res = ToolRunner.run(new Configuration(), new Fox_Martin_exercise1(), args);
		System.exit(res);
	}
} 
