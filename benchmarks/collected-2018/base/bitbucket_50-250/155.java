// https://searchcode.com/api/result/61274203/

package com.chine.kmeans.mapreduce.kmeansiter;

import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.io.Text;

import com.chine.kmeans.models.Movie;

public class KmeansIterReducer extends
		Reducer<Text, Text, Text, Text> {
	
	@Override
	public void reduce(Text key, Iterable<Text> values, Context context)
		throws IOException, InterruptedException {
		
		TreeMap<Integer, Integer> userRating = new TreeMap<Integer, Integer>();
		TreeMap<Integer, Integer> userCount = new TreeMap<Integer, Integer>();
		
		for(Text value: values) {
			String data = value.toString();
			int index = data.indexOf(":");
			int movieId = Integer.valueOf(data.substring(0, index));
			String datas = data.substring(index+1);
			
			Movie currentMovie = new Movie(movieId, datas);
			Map<Integer, Integer> features= currentMovie.getMap();
			for(Integer userId: features.keySet()) {
				if(!userRating.containsKey(userId)) {
					userRating.put(userId, 0);
					userCount.put(userId, 0);
				}
				int rating = userRating.get(userId);
				userRating.put(userId, rating+features.get(userId));
				int count = userCount.get(userId);
				userCount.put(userId, count+1);
			}
		}
		
		int acc = 0;
		StringBuilder sb = new StringBuilder();
		for(Integer userId: userRating.keySet()) {
			int rating = userRating.get(userId);
			int count = userCount.get(userId);
			
			int result = rating / count;
			if(result > 0) {
				acc++;
				sb.append(userId);
				sb.append(",");
				sb.append(result);
				sb.append(";");
			}
		}
		
		if(acc > 0) {
			String emit = sb.toString().substring(0, sb.length()-1);
			context.write(key, new Text(emit));
		}
		
	}
	
}

