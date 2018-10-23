// https://searchcode.com/api/result/36001113/

package edu.upenn.cis.bang.pagerank;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

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
import org.apache.hadoop.mapred.jobcontrol.Job;
import org.apache.hadoop.mapred.jobcontrol.JobControl;

public class PageRank {

	public static class Map extends MapReduceBase implements Mapper<LongWritable, Text, Text, Text> {
		private Text outlink = new Text();
		private Text word = new Text();
		private Text val = new Text();

		public void map(LongWritable k, Text value, OutputCollector<Text, Text> output, Reporter reporter) throws IOException {
			String line = value.toString();
			String[] split = line.split(" ");
			
			String key = split[0];
			key = key.trim();
			
			System.out.println("mapKey: "+key);
			System.out.println("map: "+line);
			
			double pageRank = 0.0;
			try {
				pageRank = Double.parseDouble(split[1]);
			} catch (NumberFormatException e){
				System.err.println("PageRank::Map error parsing pagerank double value");
				e.printStackTrace();
				return;
			} catch (IndexOutOfBoundsException e){
				System.err.println("PageRank:: Map Error, Index out of bounds exception on line = "+line);
				return;
			}
			
			
			int numOutlinks = split.length-2;
			// note i starts at 2!
			for (int i = 2; i < split.length; i++){
				if (!split[i].equals(key)){
					outlink.set(split[i].trim());
					String str = (key.toString() + " " + String.valueOf(pageRank) + " " + String.valueOf(numOutlinks)).trim();
					word.set(str);
					output.collect(outlink, word);
				}
			}
			
			StringBuffer sb = new StringBuffer();
			for (int i = 1; i < split.length; i++){
				sb.append(split[i]+" ");
			}
			val.set(sb.toString());
			// also emit all the outlinks
			output.collect(new Text(key), val);
		}
	}

	public static class Reduce extends MapReduceBase implements Reducer<Text, Text, Text, Text> {
		private Text word = new Text();
		static Double DAMPING_FACTOR = 0.85;
		static Double NORMALIZE_FACTOR = 1.0;
		
		public void reduce(Text key, Iterator<Text> values, OutputCollector<Text, Text> output, Reporter reporter) throws IOException {
			
			String allOutlinks = "";
			double pageRank = 0.0;
			double currentPageRank = 0.0;
			List<String> allInlinks = new LinkedList<String>();
			
			boolean hasChangedPageRank = false;
			
			System.out.println("reduceKey: "+key);
			while (values.hasNext()) {
				Text value = values.next();
				System.out.println("reduce: "+value.toString());
				
				String[] split = value.toString().split(" ");
				
				try {
					String inbound = split[0];
					inbound = inbound.trim();
					
					// if inbound is a pagerank value, then 'value' represents outbound links
					try {
						double d = Double.parseDouble(inbound);
						allOutlinks = value.toString();
						currentPageRank = d;
						System.out.println("setting current pagerank to "+currentPageRank);
						// we are done, continue
						continue;	
					} catch (NumberFormatException e){
					}
					allInlinks.add(inbound);
					double inboundPagerank = Double.parseDouble(split[1]);
					double numOutlinks = Double.parseDouble(split[2]);
					
					pageRank += (DAMPING_FACTOR*(inboundPagerank/numOutlinks));
					hasChangedPageRank = true;
					
				} catch (NumberFormatException e){
					System.err.println("PageRank::Reduce error parsing double");
					e.printStackTrace();
				} catch (IndexOutOfBoundsException e){
					System.err.println("PageRank::Reduce value index out of bounds");
					e.printStackTrace();
				}
			} // end while
			
			// note that allOutlinks[0] is still the old pagerank value, replace it with new one
			String[] spl = allOutlinks.split(" ");
			StringBuffer sb = new StringBuffer();
			
			// if we do have outbound links
			if (spl.length > 1){
				for (int i = 1; i < spl.length; i++){
					sb.append(spl[i]+" ");
				}
			}
			// else there are no outbound links, we consider the page to refer back to all inbound links instead
			else {
				for (String in : allInlinks){
					sb.append(in+" ");
				}
			}
			
			pageRank += (1.0-DAMPING_FACTOR)/NORMALIZE_FACTOR;
			
			if (!hasChangedPageRank){
				pageRank = currentPageRank * (1.0-DAMPING_FACTOR)/NORMALIZE_FACTOR;
				System.out.println("UPDATE pagerank "+pageRank+"  "+(1.0-DAMPING_FACTOR)/NORMALIZE_FACTOR);
			}
			
			sb.insert(0, String.valueOf(pageRank)+" ");
			
			System.out.println("ReduceOutput: "+key.toString() + " " + sb.toString().trim());
			
			word.set(" "+(sb.toString().trim()));
			output.collect(key, word);
		}
	}

	public static void runJob(String input, String output) throws IOException{
		JobConf conf1 = new JobConf(PageRank.class);
		conf1.setJobName("pagerank");

		conf1.setOutputKeyClass(Text.class);
		conf1.setOutputValueClass(Text.class);

		conf1.setMapperClass(Map.class);
		//conf1.setCombinerClass(Reduce.class);
		conf1.setReducerClass(Reduce.class);

		conf1.setInputFormat(TextInputFormat.class);
		conf1.setOutputFormat(TextOutputFormat.class);

		FileInputFormat.setInputPaths(conf1, new Path(input));
		FileOutputFormat.setOutputPath(conf1, new Path(output));

		System.out.println("Running PageRank job, damping=" + Reduce.DAMPING_FACTOR + ", normalize=" + Reduce.NORMALIZE_FACTOR +"  with input>>"+input+"  output>>"+output);
		
		JobClient.runJob(conf1);
	}
	
	public static void printUsage(){
		System.out.println("Usage: <input path> <output path> [num iterations] [normalize factor] [damping factor]");
	}

	public static void main(String[] args) throws Exception {
		if (args.length < 2 || args.length > 5){
			printUsage();
			System.exit(0);
		}
		
		int numIterations = 1;
		if (args.length > 2){
			numIterations = Integer.parseInt(args[2]);
		}
		
		if (args.length > 3){
			double norm = Double.parseDouble(args[3]);
			Reduce.NORMALIZE_FACTOR = norm;
		}
		
		if (args.length > 4){
			double damp = Double.parseDouble(args[4]);
			if (damp < 0 || damp > 1){
				printUsage();
				System.exit(0);
			}
			Reduce.DAMPING_FACTOR = damp;
		}
		
		String input = args[0];
		String output = args[1];
		for (int i = 0; i < numIterations; i++){
			runJob(input+i, output+(i+1));
		}
		
	}
}


