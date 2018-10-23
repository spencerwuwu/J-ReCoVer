// https://searchcode.com/api/result/124699289/

/**
 * 
 */
package org.jaggu.hadoop.goose;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;

import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.mapred.JobClient;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.MapReduceBase;
import org.apache.hadoop.mapred.Mapper;
import org.apache.hadoop.mapred.OutputCollector;
import org.apache.hadoop.mapred.Reducer;
import org.apache.hadoop.mapred.Reporter;
import org.apache.hadoop.mapred.TextOutputFormat;
import org.apache.hadoop.mapred.lib.db.DBConfiguration;
import org.apache.hadoop.mapred.lib.db.DBInputFormat;
import org.apache.hadoop.mapred.lib.db.DBOutputFormat;
import org.apache.hadoop.mapred.lib.db.DBWritable;

import com.gravity.goose.Article;
import com.gravity.goose.Configuration;
import com.gravity.goose.Goose;

/**
 * @author Jaganadh G
 * 
 */
public class GooseHadoop {

	// Mapper BEGIN
	public static class GooseExtractMapper extends MapReduceBase
			implements
			Mapper<LongWritable, ContentExtractDB, LongWritable, Text> {

		public void map(LongWritable key, ContentExtractDB val,
				OutputCollector<LongWritable, Text> output,
				Reporter reporter) {
			//|^ OutputCollector<LongWritable, ContentExtractDB> output,
			
			Goose goose = new Goose(new Configuration());
			//extractdb.content = new Text(extractdb.content);
			
			
			try {
				Text article = new Text();
				ContentExtractDB extractdb = new ContentExtractDB();
				//extractdb.id = val.id;
				//extractdb.url = val.url;
				//article = new Text(goose.extractContent(val.url.toString())
						//.toString());
				///
				article = new Text(goose.extractContent(val.url.toString()).toString());
				//extractdb.content = new Text(article);
				//extractdb.content = goose.extractContent(val.url.toString())
				//		.toString();
				//extractdb.content = article.toString();
				output.collect(new LongWritable(val.id), article);
				//output.collect(new LongWritable(val.id), extractdb);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	// Mapper END

	// Reducer BEGIN
	public static class GooseExtractorReduce extends MapReduceBase implements
			Reducer<LongWritable, ContentExtractDB, ContentExtractDB, Text> {
		public void reduce(LongWritable key, Iterator<ContentExtractDB> val,
				OutputCollector<ContentExtractDB, Text> output,
				Reporter reporter) {
			ContentExtractDB context;
			while (val.hasNext()) {
				context = val.next();
				try {
					output.collect(context, new Text(context.content.toString()));
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	// Reducer END

	public static class ContentExtractDB implements Writable, DBWritable {
		int id;
		String url;
		String content;

		public ContentExtractDB() {
		}

		public void readFields(ResultSet resultSet) throws SQLException {
			this.id = resultSet.getInt(1);
			this.url = resultSet.getString(2);

		}

		public void write(PreparedStatement prepstatement) throws SQLException {
			prepstatement.setInt(1, this.id);
			prepstatement.setString(2, this.url);
			prepstatement.setString(3, this.content);

		}

		public void readFields(DataInput datain) throws IOException {
			this.id = datain.readInt();
			this.url = Text.readString(datain);
		}

		public void write(DataOutput out) throws IOException {
			out.writeInt(this.id);
			Text.writeString(out, this.url);
			Text.writeString(out, this.content.toString());

		}

	}

	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		JobConf conf = new JobConf(GooseHadoop.class);
		conf.setJobName("goose_hadoop");
		conf.setJarByClass(GooseHadoop.class);
		conf.setInputFormat(DBInputFormat.class);
		conf.setOutputFormat(DBOutputFormat.class);
		// ContentExtract
		DBConfiguration.configureDB(conf, "com.mysql.jdbc.Driver",
				"jdbc:mysql://localhost/ContentExtract", "jaganadhg",
				"jagan123");

		conf.setMapperClass(GooseExtractMapper.class);
		conf.setCombinerClass(GooseExtractorReduce.class);
		conf.setReducerClass(GooseExtractorReduce.class);

		//conf.setOutputKeyClass(DBOutputFormat.class);
		//conf.setOutputValueClass(DBOutputFormat.class);

		String[] fields = { "id", "url","content" };
		

		DBInputFormat.setInput(conf, ContentExtractDB.class, "Content",
				null /* conditions */, "id", fields);
		DBOutputFormat.setOutput(conf, "Content", "id", "url", "content");

		JobClient.runJob(conf);

	}

}

