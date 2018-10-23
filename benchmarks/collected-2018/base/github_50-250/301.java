// https://searchcode.com/api/result/97674942/

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
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
import edu.umd.cloud9.io.pair.*;

import org.apache.commons.collections.MultiMap;
import org.apache.commons.collections.map.MultiValueMap;



public class DocPositions {
	private static final String[] STOP = {"the", "of", "and", "in", "to", "a", "is", "the", "as", "by",
		"that", "for", "was", "with","are","on", "from", "or", "an", "his", "be",
		"which", "at", "have", "it", "not", "were", "has", "also", "he", "but", "one",
		"had", "other", "their", "this", "its", "been", "such", "first", "more", "used",
		"can", "all", "they", "who", "than", "some", "most", "into", "only", "many",
		"two", "many", "would", "she", "he"};
	private static final HashSet<String> STOP_WORDS = new HashSet<String>(Arrays.asList(STOP));

	public static class DocPositionsMapper extends MapReduceBase
	implements Mapper<LongWritable, WikipediaPage, Text, PairOfStringInt> {
	
		private Text word = new Text();
		private int totalWords;
		private PairOfStringInt id_pos = new PairOfStringInt();
		
		public void map(LongWritable key, WikipediaPage value,
				OutputCollector<Text, PairOfStringInt>	output, Reporter report)
				throws IOException {
			
			if (value.isArticle()) {
				String articleID = value.getDocid();
				totalWords = 0;
				
				String content = value.getContent();
				StringTokenizer st = new StringTokenizer(content);
			    
				while (st.hasMoreTokens()){
			    	totalWords++;
			    	String s = st.nextToken();
			    	
			    	ArrayList<String> words = scrubWords(s);
			    	
			    	for (String s2: words) {
			    		if(!STOP_WORDS.contains(s2.toLowerCase())){	
			    			word.set(s2);
				    		id_pos.set(articleID, totalWords);
				    		output.collect(word, id_pos);
			    		}
			    	}
			    }
			}
		}
	}

	public static class DocPositionsReducer extends MapReduceBase
		implements Reducer<Text, PairOfStringInt, Text, Text> {
		
		public void reduce (Text key, Iterator<PairOfStringInt> values,
				OutputCollector<Text, Text> output, Reporter report)
				throws IOException {
			
			/*PairOfStrings id_pos = new PairOfStrings();
			
			Set<String> articlesSet = new HashSet<String>();
			Text articleID = new Text();
			String id = "";
			String pos = "";
			
			while (values.hasNext()) {
				String temp = values.next().toString();
				articlesSet.add(id);
			}

			articleID.set(id);*/
			
			MultiMap multiMap = new MultiValueMap();
			Text out = new Text();
			
			
			while (values.hasNext()) {
				
				PairOfStringInt id_pos = values.next();
				String id = id_pos.getLeftElement();
				int pos = id_pos.getRightElement();
				multiMap.put(id, pos);
			}
			
			@SuppressWarnings("unchecked")
			Set<String> keys = multiMap.keySet();
			for (String mapkey : keys) {
				out.set("(" + mapkey + ", " + multiMap.get(mapkey).toString() + ")");
				output.collect(key, out);
			}
			

		}
	}

	public static void main (String[] args) throws Exception {
		JobConf conf = new JobConf(DocPositions.class);
		conf.setJobName("Positions");
		
		conf.setInputFormat(WikipediaPageInputFormat.class);
		conf.setOutputFormat(TextOutputFormat.class);
		
		conf.setMapperClass(DocPositionsMapper.class);
		conf.setReducerClass(DocPositionsReducer.class);
		conf.setMapOutputKeyClass(Text.class);
		conf.setMapOutputValueClass(PairOfStringInt.class);
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
		int i = 0;
		ArrayList<String> end = new ArrayList<String>();
		char letter;
		String wordEnd = "";
		
		while(i < word.length())
		{
			letter = word.charAt(i);
			if(isAcceptable(letter))
			{
				wordEnd = wordEnd + letter;
				
				if (i == word.length() - 1)
				{
					wordEnd = cleanup(wordEnd);
					
					if(!wordEnd.isEmpty())
						end.add(wordEnd);
				}
			}
			else
			{
				
				wordEnd = cleanup(wordEnd);
				
				if(!wordEnd.isEmpty())
					end.add(wordEnd);
				
				wordEnd = "";
			}
			i++;
		}
		
		
		return end;
	}
	
	public static boolean isAcceptable(char letter)
	{
		if(Character.isLetterOrDigit(letter) || letter == 39)
		{
			return true;
		}
		return false;
	}
	
	// different and cleaner, we'll see if it works
	public static String cleanup(String word)
	{
		while(word.length() > 0 && word.startsWith("'"))
			word = word.substring(1);
		
		while(word.length() > 1 && word.endsWith("'"))
			word = word.substring(0, word.length()-1);
		
		if(word.length() > 1 && word.endsWith("'s"))
			word = word.substring(0, word.length()-2);
		
		return word;
	}
}

