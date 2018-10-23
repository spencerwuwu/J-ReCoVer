// https://searchcode.com/api/result/97674963/

import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.StringTokenizer;

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
import org.apache.hadoop.mapred.TextOutputFormat;

import edu.umd.cloud9.collection.wikipedia.*;



public class WordCount {

	public static class InvertedIndexMapper extends MapReduceBase
	implements Mapper<LongWritable, WikipediaPage, Text, Text> {
	
	private Text id = new Text();
	private Text word = new Text();

		public void map(LongWritable key, WikipediaPage value,
				OutputCollector<Text, Text>	output, Reporter report)
				throws IOException {
			
			if (value.isArticle()) {
				String articleID = value.getDocid();
				id.set(articleID);
				
				String content = value.getContent();
				StringTokenizer st = new StringTokenizer(content);
			    while (st.hasMoreTokens()){
			    	String s = st.nextToken();
			    	word.set(s);
			    	output.collect(word, id);
			    }
			}
		}
	}

	public static class InvertedIndexReducer extends MapReduceBase
		implements Reducer<Text, Text, Text, Text> {
		
		private int count;
		
		public void reduce (Text key, Iterator<Text> values,
				OutputCollector<Text, Text> output, Reporter report)
				throws IOException {
			
			count = 0;
			
			Set<String> articlesSet = new HashSet<String>();
			Text articleID = new Text();
			
			while (values.hasNext()) {
				articlesSet.add(values.next().toString());
				count++;
			}
			
			String id = "count: " + count;
			
			for (String s : articlesSet) {
				id += s + ", ";
			}


			articleID.set(id);
			
			output.collect(key, articleID);
			
		}
	}

	public static void main (String[] args) throws Exception {
		JobConf conf = new JobConf(WordCount.class);
		conf.setJobName("WordCount");
		
		conf.setInputFormat(WikipediaPageInputFormat.class);
		conf.setOutputFormat(TextOutputFormat.class);
		
		conf.setMapperClass(InvertedIndexMapper.class);
		conf.setReducerClass(InvertedIndexReducer.class);
		conf.setMapOutputKeyClass(Text.class);
		conf.setMapOutputValueClass(Text.class);
		conf.setOutputKeyClass(Text.class);
		conf.setOutputValueClass(Text.class);
		
		FileInputFormat.setInputPaths(conf, new Path(args[0]));
		FileOutputFormat.setOutputPath(conf, new Path(args[1]));
		
		long startTime = System.currentTimeMillis();
		JobClient.runJob(conf);
		System.out.println("Job finished in: " + (System.currentTimeMillis() - startTime) / 1000 + " seconds");
	}
}

