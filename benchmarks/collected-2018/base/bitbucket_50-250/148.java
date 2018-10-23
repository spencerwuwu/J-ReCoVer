// https://searchcode.com/api/result/61274207/

package com.chine.kmeans.mapreduce.canopymaker;

import java.io.IOException;
import java.util.List;
import java.util.ArrayList;

import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.io.Text;

import com.chine.kmeans.models.Movie;
import com.chine.kmeans.mapreduce.ConfiguredKmeans;

public class CanopyMakerReducer extends Reducer<Text, Text, Text, Text> {

	private List<Movie> canopyMovieCenters;

	@Override
	public void setup(Context context) {
		this.canopyMovieCenters = new ArrayList<Movie>();
	}

	@Override
	public void reduce(Text key, Iterable<Text> values, Context context)
			throws IOException, InterruptedException {

		for (Text value : values) {
			String[] movieAndData = value.toString().split(":");
			int movieId = Integer.valueOf(movieAndData[0]);
			String data = movieAndData[1].toString();

			Movie currentMovie = new Movie(movieId, data);

			boolean tooClose = false;
			for (Movie m : canopyMovieCenters) {
				if (m.getMatchCount(currentMovie) >= ConfiguredKmeans.T1) {
					tooClose = true;
					break;
				}
			}

			if (!tooClose) {
				canopyMovieCenters.add(currentMovie);
				context.write(new Text(String.valueOf(movieId)), new Text(data));
			}
		}

	}

}

