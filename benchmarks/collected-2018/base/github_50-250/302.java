// https://searchcode.com/api/result/97674967/

import java.io.IOException;
import java.util.ArrayList;
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
import edu.umd.cloud9.io.pair.Pair;



public class WCScrub {

	public static class InvertedIndexMapper extends MapReduceBase
	implements Mapper<LongWritable, WikipediaPage, Text, Text> {
	
	private Text id = new Text();
	private Text word = new Text();
	private int totalWords;
		public void map(LongWritable key, WikipediaPage value,
				OutputCollector<Text, Pair>	output, Reporter report)
				throws IOException {
			
			if (value.isArticle()) {
				String articleID = value.getDocid();
				Pair out = new Pair();
				totalWords = 0;
				String content = value.getContent();
				StringTokenizer st = new StringTokenizer(content);
			    while (st.hasMoreTokens()){
			    	totalWords++;
			    	String s = st.nextToken();
			    	
			    	ArrayList<String> words = scrubWords(s);
			    	
			    	for(String s2: words)
			    	{
			    		word.set(s2);
			    		out.of(articleID, totalWords);
			    		output.collect(word, id);
			    	}
			    	
			    	/*
			    	word.set(s);
			    	output.collect(word, id);
			    	*/
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
			String id = "";
			while (values.hasNext()) {
				id += values.next().toString();
				articlesSet.add(id);
				count++;
			}
			
			id += " total word count: " + count;

			articleID.set(id);
			
			output.collect(key, articleID);
			
		}
	}

	public static void main (String[] args) throws Exception {
		JobConf conf = new JobConf(WCScrub.class);
		conf.setJobName("WordCount2");
		
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
	
	
	public static ArrayList<String> scrubWords(String word)
	{
		//word = word.toLowerCase();
		int i = 0;
		ArrayList<String> end = new ArrayList<String>();
		char letter;
		String wordEnd = "";
		
		while(i < word.length())
		{
			letter = word.charAt(i);
			
			if(Character.isLetterOrDigit(letter) || letter == 39)
			{
				wordEnd = wordEnd + letter;
				
				if (i == word.length() - 1 && !wordEnd.equals("")) {
					wordEnd = cleanup(wordEnd);
					end.add(wordEnd);
				}
			}
			else
			{
				if(wordEnd.length() > 0)
				{
					cleanup(wordEnd);
					end.add(wordEnd);
				}
				wordEnd = "";
			}
			i++;
		}
		
		
		return end;
	}
	
	
	public boolean isLetter(char letter)
	{
		if(letter >= 97 && letter <= 122 || letter >= 65 && letter <= 90)
		{
			return true;
		}
		return false;
	}
	
	public static String cleanup(String word)
	{
		while(word.length() > 0 && !Character.isLetterOrDigit(word.charAt(0)))
			word = word.substring(1);
		
		while(word.length() > 1 && !Character.isLetterOrDigit(word.charAt(word.length()-1)))
			word = word.substring(0, word.length()-1);
		
		if(word.length() > 1 && word.substring(word.length()-2).equals("'s"))
			word = word.substring(0, word.length()-2);
		
		return word;
	}
}

