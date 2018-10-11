// https://searchcode.com/api/result/99975919/

import java.io.*;
import java.util.*;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.conf.*;
import org.apache.hadoop.io.*;
import org.apache.hadoop.mapred.*;
import org.apache.hadoop.util.*;
import org.apache.hadoop.mapred.lib.*;

public class Li_Lullo_Martin_exercise1 extends Configured implements Tool {
	public static class Map1 extends MapReduceBase implements Mapper<LongWritable, Text, Text, DoubleWritable> {
		private DoubleWritable numVols = new DoubleWritable();
		private Text combo = new Text();

		public void configure(JobConf job) {

		}

		public void map(LongWritable key, Text inputLine, OutputCollector<Text, DoubleWritable> output, Reporter reporter) throws IOException {
			String line = inputLine.toString();
			StringTokenizer tokenizer = new StringTokenizer(line);
	    	String word = tokenizer.nextToken();
	    	String year = tokenizer.nextToken();
	    	tokenizer.nextToken(); //# occurrences - don't need
	    	numVols.set(Double.parseDouble(tokenizer.nextToken()));


			if(checkYear(year)) {
				String substring = "";
				boolean hasSubstring = false;
				if(word.toLowerCase().contains("nu")) {
					hasSubstring = true;
					substring = "nu";
				}
				else if(word.toLowerCase().contains("die")) {
					hasSubstring = true;
					substring = "die";
				}
				else if(word.toLowerCase().contains("kla")) {
					hasSubstring = true;
					substring = "kla";
				}

				if(hasSubstring) {
					combo.set(year + " " + substring);
					output.collect(combo, numVols);
				}
			}
		}

		public boolean checkYear(String year) {
		    try
		    {
		        Integer.parseInt(year);
		        return true;
		    } catch (NumberFormatException ex)
		    {
		        return false;
		    }
		}
	}	

	public static class Map2 extends MapReduceBase implements Mapper<LongWritable, Text, Text, DoubleWritable> {
		private DoubleWritable numVols = new DoubleWritable();
		private Text combo = new Text();

		public void configure(JobConf job) {

		}

		public void map(LongWritable key, Text inputLine, OutputCollector<Text, DoubleWritable> output, Reporter reporter) throws IOException {
			String line = inputLine.toString();
			StringTokenizer tokenizer = new StringTokenizer(line);
	    	String word = tokenizer.nextToken()+" "+tokenizer.nextToken();
	    	String year = tokenizer.nextToken();
	    	tokenizer.nextToken(); //# occurrences - don't need
	    	numVols.set(Double.parseDouble(tokenizer.nextToken()));

			if(checkYear(year)) {
				String substring = "";
				boolean hasSubstring = false;
				if(word.toLowerCase().contains("nu")) {
					hasSubstring = true;
					substring = "nu";
				}
				else if(word.toLowerCase().contains("die")) {
					hasSubstring = true;
					substring = "die";
				}
				else if(word.toLowerCase().contains("kla")) {
					hasSubstring = true;
					substring = "kla";
				}

				if(hasSubstring) {
					combo.set(year + " " + substring);
					output.collect(combo, numVols);
				}
			}
		}

		public boolean checkYear(String year) {
		    try
		    {
		        Integer.parseInt(year);
		        return true;
		    } catch (NumberFormatException ex)
		    {
		        return false;
		    }
		}

	}	

	public static class Reduce extends MapReduceBase implements Reducer<Text, DoubleWritable, Text, DoubleWritable> {
		public void reduce(Text key, Iterator<DoubleWritable> values, OutputCollector<Text, DoubleWritable> output, Reporter reporter) throws IOException {
			double avgVol = 0;
			double sum = 0;
			double count = 0;
			while(values.hasNext()){
				sum += values.next().get();
				count++;
			}
			avgVol = sum/count;
			output.collect(key, new DoubleWritable(avgVol));
		}
	}


	public int run(String[] args) throws Exception{
		JobConf conf = new JobConf(getConf(), Li_Lullo_Martin_exercise1.class);
		conf.setJobName("hw2ex1");

		conf.setOutputKeyClass(Text.class);
		conf.setOutputValueClass(DoubleWritable.class);

		conf.setMapperClass(Map1.class);
		conf.setMapperClass(Map2.class);
		conf.setCombinerClass(Reduce.class);
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
		int res = ToolRunner.run(new Configuration(), new Li_Lullo_Martin_exercise1(), args);
		System.exit(res);
	}
} 
