// https://searchcode.com/api/result/97674979/

import java.io.IOException;
import java.util.*;
import java.util.Iterator;
import java.util.StringTokenizer;

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
import org.apache.hadoop.mapred.TextOutputFormat;

import edu.umd.cloud9.collection.wikipedia.*;



public class ScrubWords {
	private static final String[] STOP = {"the", "of", "and", "in", "to", "a", "is", "the", "as", "by",
			"that", "for", "was", "with","are","on", "from", "or", "an", "his", "be",
			"which", "at", "have", "it", "not", "were", "has", "also", "he", "but", "one",
			"had", "other", "their", "this", "its", "been", "such", "first", "more", "used",
			"can", "all", "they", "who", "than", "some", "most", "into", "only", "many",
			"two", "many", "would", "she", "he"};
	private static final HashSet<String> STOP_WORDS = new HashSet<String>(Arrays.asList(STOP));
	
	public static class ScrubWordsMapper extends MapReduceBase
	implements Mapper<LongWritable, WikipediaPage, Text, IntWritable> {
	
		private Text word = new Text();
		private final static IntWritable one = new IntWritable(1);

		public void map(LongWritable key, WikipediaPage value,
				OutputCollector<Text, IntWritable>	output, Reporter report)
				throws IOException {
			
			if (value.isArticle()) {
				
				String content = value.getContent();
				StringTokenizer st = new StringTokenizer(content);
			    
				while (st.hasMoreTokens()){
			    	String s = st.nextToken();
			    	
			    	ArrayList<String> words = scrubWords(s);
			    	
			    	for (String s2: words) {
			    		if(!STOP_WORDS.contains(s2.toLowerCase())) {
			    			word.set(s2);
			    			output.collect(word, one);
			    		}
			    	}
			    }
			}
		}
	}

	public static class ScrubWordsReducer extends MapReduceBase
		implements Reducer<Text, IntWritable, Text, IntWritable> {
		
		public void reduce (Text key, Iterator<IntWritable> values,
				OutputCollector<Text, IntWritable> output, Reporter report)
				throws IOException {
			
			int count = 0;
			
			while (values.hasNext()) {
				count += values.next().get();
			}
			
			output.collect(key, new IntWritable(count));
			
		}
	}

	public static void main (String[] args) throws Exception {
		JobConf conf = new JobConf(ScrubWords.class);
		conf.setJobName("ScrubWords");
		
		conf.setInputFormat(WikipediaPageInputFormat.class);
		conf.setOutputFormat(TextOutputFormat.class);
		
		conf.setMapperClass(ScrubWordsMapper.class);
		conf.setReducerClass(ScrubWordsReducer.class);
		conf.setMapOutputKeyClass(Text.class);
		conf.setMapOutputValueClass(IntWritable.class);
		conf.setOutputKeyClass(Text.class);
		conf.setOutputValueClass(IntWritable.class);
		
		FileInputFormat.setInputPaths(conf, new Path(args[0]));
		FileOutputFormat.setOutputPath(conf, new Path(args[1]));
		
		long startTime = System.currentTimeMillis();
		JobClient.runJob(conf);
		System.out.println("Job finished in: " + (System.currentTimeMillis() - startTime) / 1000 + " seconds");
	}
	
	
	public static ArrayList<String> scrubWords(String word)
	{

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
					wordEnd = cleanup(wordEnd);
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

