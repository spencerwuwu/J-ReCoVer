// https://searchcode.com/api/result/122936895/

package src.main.java.filter;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.WritableComparable;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.json.JSONException;

public class CombineDateIntervals {

	private static class DateWritable implements WritableComparable<DateWritable>{

		IntWritable day = new IntWritable();
		IntWritable month = new IntWritable();
		IntWritable year = new IntWritable();
		
		public DateWritable(){
			
		}
		
		public void set(int month, int day, int year){
			this.setMonth(new IntWritable(month));
			this.setDay(new IntWritable(day));
			this.setYear(new IntWritable(year));
		}
		
		@Override
		public void readFields(DataInput arg0) throws IOException {
			day.readFields(arg0);
			month.readFields(arg0);
			year.readFields(arg0);
//			day.set(arg0.readInt());
//			month.set(arg0.readInt());
//			year.set(arg0.readInt());
		}

		@Override
		public void write(DataOutput arg0) throws IOException {
//			arg0.writeInt(day.get());
//			arg0.writeInt(month.get());
//			arg0.writeInt(year.get());
			day.write(arg0);
			month.write(arg0);
			year.write(arg0);
		}

		@Override
		public int compareTo(DateWritable arg0) {
			// TODO Auto-generated method stub

			int cmp = this.year.compareTo(arg0.getYear());
			if(cmp != 0){
				return cmp;
			}
			
			cmp = this.month.compareTo(arg0.getMonth());
			if(cmp != 0){
				return cmp;
			}
			
			return this.day.compareTo(arg0.getDay());
		}
		
		@Override
		public String toString(){
			return month + "-" + day + "-" + year;
		}

		public IntWritable getDay() {
			return day;
		}

		public IntWritable getMonth() {
			return month;
		}

		public IntWritable getYear() {
			return year;
		}

		public void setDay(IntWritable day) {
			this.day = day;
		}

		public void setMonth(IntWritable month) {
			this.month = month;
		}

		public void setYear(IntWritable year) {
			this.year = year;
		}
		
	}
	
	private static class AddDatesMapper extends
			Mapper<LongWritable, Text, Text, Text> {

		String line;
		Text idText  = new Text();
		Text dateText = new Text();
		Text predictionText = new Text();
		
		@Override
		public void map(LongWritable key, Text value, Context context)
				throws IOException, InterruptedException {
			try{
				line = value.toString();
				if (line.contains("{")) { // raw json tweet
					// Must be a tweet
					idText.set(JsonUtils.getTweetId(line));
					dateText.set(JsonUtils.getDateFromTweet(line));
					if(JsonUtils.isValidDate(dateText.toString())){
						context.write(idText, dateText);
					}
				} else{ // must be a prediction
					StringTokenizer st = new StringTokenizer(line);
					idText.set(st.nextToken());
					st.nextToken(); //user id
					st.nextToken(); //gold label
					predictionText.set(st.nextToken());
					context.write(idText, predictionText);
				}
			} catch(JSONException e){
				e.printStackTrace();
			}
		}
	}

	private static class AddDatesReducer extends
			Reducer<Text, Text, Text, Text> {

		Text dateText = new Text();
		Text predText = new Text();
		
		@Override
		public void reduce(Text key, Iterable<Text> values,
				Context context) throws IOException, InterruptedException {
			String date = null;
			String prediction = null;
			for(Text text : values){
				if(JsonUtils.isValidDate(text.toString())){
					date = text.toString();
				} else if(text.toString().equals("1") || text.toString().equals("-1")){
					prediction = text.toString();
				}
			}
			
			if(date != null && prediction != null){
				dateText.set(date);
				predText.set(prediction);
				context.write(dateText, predText);
			}
			
		}

	}

	private static class CombineDatesMapper extends
			Mapper<LongWritable, Text, DateWritable, IntWritable> {

		IntWritable prediction = new IntWritable();
		DateWritable dw = new DateWritable();
		
		@Override
		public void map(LongWritable key, Text value, Context context)
				throws IOException, InterruptedException {
			try{
				StringTokenizer st = new StringTokenizer(value.toString());
				String date = st.nextToken();
				//System.out.println(date);
				int month = Integer.parseInt(date.substring(0, date.indexOf("-")));
				date = date.substring(date.indexOf("-") + 1);
				int day = Integer.parseInt(date.substring(0, date.indexOf("-")));
				date = date.substring(date.indexOf("-") + 1);
				int year = Integer.parseInt(date);	
				dw.set(month, day, year);	
				prediction.set(Integer.parseInt(st.nextToken()));
				if(dw.getDay() == null || dw.getMonth() == null || dw.getYear() == null){
					System.out.println("NULL!!!");
				} else{
					context.write(dw, prediction);
				}
			} catch(Exception e){
				return;
			}
		}
	}

	
	private static class CombineDatesReducer extends
			Reducer<DateWritable, IntWritable, DateWritable, DoubleWritable> {

		DoubleWritable dw = new DoubleWritable();
		
		@Override
		public void reduce(DateWritable key, Iterable<IntWritable> values,
				Context context) throws IOException, InterruptedException {
			int n = 0;
			int sum = 0;
			for(IntWritable iw : values){
				if(iw.get() == 1){
					sum++;
				}
				n++;
			}
			double percentApprove = 100 * ((double)sum / (double)(n));
			dw.set(percentApprove);
			context.write(key, dw);
		}
	}

	public static void main(String[] args) throws Exception {
		if (args.length == 3) {
			String input = args[0];
			String output = args[1];
			String tempInput = output + "/temp_input";
			String finalOutput = output + "/final_output";
			int reduceTasks = Integer.parseInt(args[2]);

			Configuration conf = new Configuration();
			Job job = new Job(conf, "ExtractObamaTweets");
			job.setJarByClass(ExtractObamaTweets.class);

			job.setNumReduceTasks(reduceTasks);

			FileInputFormat.setInputPaths(job, new Path(input));
			FileOutputFormat.setOutputPath(job, new Path(tempInput));

			job.setOutputKeyClass(LongWritable.class);
			job.setOutputValueClass(Text.class);

			job.setMapperClass(AddDatesMapper.class);
			job.setReducerClass(AddDatesReducer.class);
			
			job.setMapOutputKeyClass(Text.class);
			job.setMapOutputValueClass(Text.class);
			job.setOutputKeyClass(Text.class);
			job.setOutputValueClass(Text.class);

//			// Delete the output directory if it exists already
			Path outputDir = new Path(tempInput);
//			FileSystem.get(conf).delete(outputDir, true);
//
//			job.waitForCompletion(true);
			
			/**
			 * Combine date intervals
			 */
			conf = new Configuration();
			job = new Job(conf, "ExtractObamaTweets");
			job.setJarByClass(ExtractObamaTweets.class);

			job.setNumReduceTasks(reduceTasks);

			FileInputFormat.setInputPaths(job, new Path(tempInput));
			FileOutputFormat.setOutputPath(job, new Path(finalOutput));

			job.setMapperClass(CombineDatesMapper.class);
			job.setReducerClass(CombineDatesReducer.class);
			
			job.setMapOutputKeyClass(DateWritable.class);
			job.setMapOutputValueClass(IntWritable.class);
			job.setOutputKeyClass(DateWritable.class);
			job.setOutputValueClass(DoubleWritable.class);
			
			// Delete the output directory if it exists already
			outputDir = new Path(finalOutput);
			FileSystem.get(conf).delete(outputDir, true);
			
			job.waitForCompletion(true);

		}
	}

}

