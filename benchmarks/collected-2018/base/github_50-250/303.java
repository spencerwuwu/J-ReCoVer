// https://searchcode.com/api/result/97674971/

import java.io.IOException;
import java.util.*;

import javax.security.auth.login.Configuration;

import org.apache.hadoop.fs.FileSystem;

import org.apache.hadoop.fs.Path;
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

import edu.umd.cloud9.io.pair.Pair;
import edu.umd.cloud9.io.pair.PairOfStrings;



public class Query {
	public static Stack<String> qstack = new Stack<String>();
	public static class QueryMapper extends MapReduceBase
	implements Mapper<LongWritable, Text, Text, Text> {
	
	private Text currentWord = new Text();
	private String query;
	
		public void configure(JobConf conf){
			query = conf.get("parameter");
		}
	
		public void map(LongWritable key, Text value,
				OutputCollector<Text, Text>	output, Reporter report)
				throws IOException {
			
			StringTokenizer parse = new StringTokenizer(query);
			HashSet<String> include = new HashSet<String>();
			
			boolean not = true;
			boolean and = false;
			String prev = "";
			while(parse.hasMoreTokens()){
				String current = parse.nextToken();
				while(current.equals("not")){
					if (not){
						not = false;
					}
					else
						not = true;
					//qstack.push(current);
					current = parse.nextToken();
				}
				if (not && !current.equals("and") && !current.equals("or")){
					//qstack.push(current);
					include.add(current);
				} 
				prev = current;
			}
			String line = value.toString();
			Scanner tokenizer = new Scanner(line);
      		String word = tokenizer.next().toLowerCase();
      		
      		if (include.contains(word)){
      			output.collect(new Text(word), new Text(tokenizer.nextLine()));
      		} 
			
		}
	}

	public static class QueryReducer extends MapReduceBase
		implements Reducer<Text, Text, Text, Text> {
		
		private String query;
		public void configure(JobConf conf){
			query = conf.get("parameter");
		}
		
		public void reduce (Text key, Iterator<Text> values,
				OutputCollector<Text, Text> output, Reporter report)
				throws IOException {
			boolean check = true;
			Set<String> wordSet = new HashSet<String>();
			
			while(values.hasNext() && check){
				String current = values.next().toString();
				wordSet.add(current);
				if (current.startsWith("not ")){
					check = false;
				}
				
			}
			
			if (check){
				String wordList = "";
				for (String word: wordSet){
					wordList = wordList + word +", ";
				}
			output.collect(key, new Text(wordList));
			}
		}
	}

	public static void main (String[] args) throws Exception {
		JobConf conf = new JobConf(Query.class);
		conf.setJobName("InvertedIndex");
		
		System.out.print("Enter query: ");
		Scanner scanner = new Scanner(System.in);
		conf.set("parameter", scanner.nextLine());
		
		conf.setInputFormat(TextInputFormat.class);
		conf.setOutputFormat(TextOutputFormat.class);
		
		conf.setMapperClass(QueryMapper.class);
		conf.setReducerClass(QueryReducer.class);
		conf.setMapOutputKeyClass(Text.class);
		conf.setMapOutputValueClass(Text.class);
		conf.setOutputKeyClass(Text.class);
		conf.setOutputValueClass(Text.class);
		
		FileInputFormat.setInputPaths(conf, new Path(args[0]));
		FileOutputFormat.setOutputPath(conf, new Path(args[1]));
		
		FileSystem.get(conf).delete(new Path("query_out"));
		long startTime = System.currentTimeMillis();
		JobClient.runJob(conf);
		System.out.println("Job finished in :" + (System.currentTimeMillis() - startTime) / 1000 + " seconds");
	}
}
