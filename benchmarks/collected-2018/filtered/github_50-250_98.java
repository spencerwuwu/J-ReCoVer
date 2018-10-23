// https://searchcode.com/api/result/68086003/

import java.io.IOException;
import java.util.*;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.conf.*;
import org.apache.hadoop.io.*;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.mapred.*;
import org.apache.hadoop.util.*;
import java.io.DataInput;
import java.io.DataOutput;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.apache.hadoop.mapred.lib.db.DBWritable;
import org.apache.hadoop.mapred.lib.IdentityReducer;
import org.apache.hadoop.mapred.lib.db.DBConfiguration;
import org.apache.hadoop.mapred.lib.db.DBOutputFormat;
import org.apache.hadoop.mapred.lib.db.DBInputFormat;
import com.mysql.jdbc.Driver;

/*
MySQL DB Schema:

DROP TABLE IF EXISTS `school`.`teacher`;
CREATE TABLE  `WordCount`.`Counting` (
`name` char(24) default NULL,
`count` int(11) default NULL
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

*/

public class DBCountPageView {
    // Output Record Object
    static class WordCountInfoRecord implements Writable,  DBWritable
    {
        public String name;
        public int count;
        public WordCountInfoRecord() {
        }

        public WordCountInfoRecord(String str, int c)
        {
            this.name = str;
            this.count = c;
        }

        public void readFields(DataInput in) throws IOException {
            this.name = Text.readString(in);
            this.count = in.readInt();
        }
        public void write(DataOutput out) throws IOException {
            Text.writeString(out, this.name);
            out.writeInt(this.count);
        }

        public void readFields(ResultSet result) throws SQLException {
            this.name = result.getString(1);
            this.count = result.getInt(2);
        }
        public void write(PreparedStatement stmt) throws SQLException {
            stmt.setString(1, this.name);
            stmt.setInt(2, this.count);
        }
        public String toString() {
            return new String(this.name + " " + this.count);
        }
    }
    
    
    public static class Map extends MapReduceBase implements Mapper<LongWritable, Text, Text, IntWritable> {

      private final static IntWritable one = new IntWritable(1);
      private Text word = new Text();

      public void map(LongWritable key, Text value, OutputCollector<Text, IntWritable> output, Reporter reporter) throws IOException
      {
        String line = value.toString();
        StringTokenizer tokenizer = new StringTokenizer(line);
        while (tokenizer.hasMoreTokens()) {
          word.set(tokenizer.nextToken());
          output.collect(word, one);
        }

      }
    }

    public static class Reduce extends MapReduceBase implements Reducer<Text, IntWritable, WordCountInfoRecord, NullWritable>
    {
      public void reduce(Text key, Iterator<IntWritable> values, OutputCollector<WordCountInfoRecord, NullWritable> output, Reporter reporter) throws IOException
      {
        int sum = 0;
        while (values.hasNext()) {
          sum += values.next().get();
        }
        // Output Data into MySQL
        output.collect(new WordCountInfoRecord(key.toString(),sum), NullWritable.get());
      }
    }

    public static void main(String[] args) throws Exception {

        // The following is basic wordcount
        JobConf conf = new JobConf(DBCountPageView.class);
        conf.setJobName("MySQL DB Wordcount");


        Class.forName("com.mysql.jdbc.Driver");

        conf.setInputFormat(TextInputFormat.class);
        conf.setOutputFormat(DBOutputFormat.class);
        FileInputFormat.setInputPaths(conf, new Path(args[0]));
        // Setup MySQL Connection , default account:root , no password
        String[] MyDBPath={"jdbc:mysql://localhost/WikiTest","root", ""};
        DBConfiguration.configureDB(conf, "com.mysql.jdbc.Driver", MyDBPath[0], MyDBPath[1], MyDBPath[2]);
        // Setup Output MySQL Format
        String[] fields = { "name", "count" };
        //DBOutputFormat.setOutput(conf, "Counting(name,count)", fields); //this line contains the name of database
        DBOutputFormat.setOutput(conf, "Counting", fields);
        
        // Set Mapper and Reducer Class
        conf.setMapperClass(Map.class);
        //conf.setCombinerClass(Reduce.class);
        conf.setReducerClass(Reduce.class);
        // I've tried all combinations , but the bug still happen.

        conf.setMapOutputKeyClass(Text.class);
        conf.setMapOutputValueClass(IntWritable.class);
        conf.setOutputKeyClass(WordCountInfoRecord.class);
        conf.setOutputValueClass(NullWritable.class);

        JobClient.runJob(conf);
    }
}
