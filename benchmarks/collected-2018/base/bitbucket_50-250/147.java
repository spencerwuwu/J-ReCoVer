// https://searchcode.com/api/result/61274201/

package com.chine.kmeans.mapreduce.resultsview;

import java.io.IOException;
import java.util.HashMap;

import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.SequenceFile;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.conf.Configuration;

import com.chine.kmeans.mapreduce.ConfiguredKmeans;

public class ResultViewReducer extends
		Reducer<Text, Text, Text,Text> {
	
	private HashMap<Integer, String> movieIdToTitles = new HashMap<Integer, String>();
	private boolean hasLoadTitles = false;
	
	@Override
	public void setup(Context context) throws IOException {
		if(hasLoadTitles) return;
		else hasLoadTitles = true;
		
		Configuration conf = context.getConfiguration();
		FileSystem fs = FileSystem.get(conf);
		
		String path = conf.get(ConfiguredKmeans.MOVIE_TITLE_OUTPUT_KEY);
		path = path.endsWith("/") ? path + "part-r-00000": path + "/part-r-00000";
		Path movieTitlePath = new Path(path);
		SequenceFile.Reader reader = new SequenceFile.Reader(fs, movieTitlePath, conf);
		
		Text key = new Text();
		Text value = new Text();
		while(reader.next(key, value)) {
			movieIdToTitles.put(Integer.valueOf(key.toString()), value.toString());
		}
		
	}
	
	@Override
	public void reduce(Text key, Iterable<Text> values, Context context) 
		throws IOException, InterruptedException {
		String centerTitle = movieIdToTitles.get(Integer.valueOf(key.toString()));
		
		StringBuilder sb = new StringBuilder();
		for(Text value: values) {
			Integer movieId = Integer.valueOf(Text.decode(value.getBytes(), 0, value.find(":")));
			
			sb.append(movieIdToTitles.get(movieId));
			sb.append(",");
		}
		
		if(centerTitle != null)
			context.write(new Text(centerTitle),new Text(sb.toString().substring(0, sb.length()-1)));
	}
	
}

