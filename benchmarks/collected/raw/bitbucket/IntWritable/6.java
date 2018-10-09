// https://searchcode.com/api/result/124699290/

/**
 * 
 */
package org.jaggu.hadoop.dbinput;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.mapred.JobClient;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.MapReduceBase;
import org.apache.hadoop.mapred.Mapper;
import org.apache.hadoop.mapred.OutputCollector;
import org.apache.hadoop.mapred.OutputFormat;
import org.apache.hadoop.mapred.Reducer;
import org.apache.hadoop.mapred.Reporter;
import org.apache.hadoop.mapred.SequenceFileOutputFormat;
import org.apache.hadoop.mapred.TextOutputFormat;
import org.apache.hadoop.mapred.lib.db.DBConfiguration;
import org.apache.hadoop.mapred.lib.db.DBInputFormat;
import org.apache.hadoop.mapred.lib.db.DBWritable;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.jaggu.hadoop.dbinput.TweetWordCount.GetTweets;

/**
 * @author jaganadhg
 * 
 */
public class TweetWordCount {

	// Mapper BEGIN

	public static class TweetWordCountMapper extends MapReduceBase implements
			Mapper<LongWritable, GetTweets, Text, IntWritable> {
		private final static IntWritable intTwordsCount = new IntWritable(1);
		private Text strTwoken = new Text();

		public void map(LongWritable key, GetTweets value,
				OutputCollector<Text, IntWritable> output, Reporter reporter)
				throws IOException {
			GetTweets tweets = new GetTweets();
			tweets.strTweet = value.strTweet;
			TwitterTokenizer twokenizer = new TwitterTokenizer();
			List<String> twokens = twokenizer.twokenize(value.strTweet
					.toString());

			for (int i = 0; i < twokens.size(); i++) {
				output.collect(new Text(twokens.get(i)), intTwordsCount);
			}

		}

	}

	// Mapper END

	// Reducer BEGIN
	public static class TweetWordCountReducer extends MapReduceBase implements
			Reducer<Text, IntWritable, Text, IntWritable> {
		public void reduce(Text key, Iterator<IntWritable> values,
				OutputCollector<Text, IntWritable> output, Reporter reporter)
				throws IOException {
			int intTwokenCount = 0;
			while (values.hasNext()) {
				intTwokenCount += values.next().get();
			}
			output.collect(key, new IntWritable(intTwokenCount));
		}
	}

	// Reducer END

	// DBInputFormat BEGIN
	public static class GetTweets implements Writable, DBWritable {
		String strTweet;

		public GetTweets() {

		}

		public void readFields(DataInput in) throws IOException {

			this.strTweet = Text.readString(in);
		}

		public void readFields(ResultSet resultSet) throws SQLException {
			// this.id = resultSet.getLong(1);
			this.strTweet = resultSet.getString(1);
		}

		public void write(DataOutput out) throws IOException {

		}

		public void write(PreparedStatement stmt) throws SQLException {

		}

	}

	// DBInputFormat END

	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		JobConf twokenJobConf = new JobConf(TweetWordCount.class);
		twokenJobConf.setJobName("twoken_count");

		twokenJobConf.setInputFormat(DBInputFormat.class);
		twokenJobConf.setOutputFormat(TextOutputFormat.class);

		Object out = new Path("twokens");

		twokenJobConf.setMapperClass(TweetWordCountMapper.class);
		twokenJobConf.setCombinerClass(TweetWordCountReducer.class);
		twokenJobConf.setReducerClass(TweetWordCountReducer.class);

		twokenJobConf.setOutputKeyClass(Text.class);
		twokenJobConf.setOutputValueClass(IntWritable.class);

		DBConfiguration.configureDB(twokenJobConf, "com.mysql.jdbc.Driver",
				"jdbc:mysql://localhost/GmailTrend", "jaganadhg", "jagan123");

		String[] fields = { "Tweet" };
		DBInputFormat.setInput(twokenJobConf, GetTweets.class, "NewGamil",
				null /* conditions */, "Tweet", fields);

		SequenceFileOutputFormat.setOutputPath(twokenJobConf, (Path) out);

		JobClient.runJob(twokenJobConf);

	}

}

