// https://searchcode.com/api/result/65836069/

package example.twitter;

import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * 
 */
public class ParseHashtags implements Tool
{
	/**
	 * 
	 */
	static class MyMapper extends Mapper<LongWritable,Text,Text,IntWritable>
	{
		@Override
	    public void map( LongWritable key, Text value, Mapper<LongWritable,Text,Text,IntWritable>.Context context) throws IOException, InterruptedException
	    {
	        try
	        {
	        	// Parse JSON
	        	JSONObject obj = new JSONObject(new String(value.getBytes()));
	        	//
	        	if ( obj.has("entities") )
	        	{
	        		JSONObject ents = obj.getJSONObject("entities");
	        		if ( ents.has("hashtags") )
	        		{
	        			JSONArray tags = ents.getJSONArray("hashtags");
	        			for ( int i = 0; i < tags.length(); i++ )
	        			{
	        				JSONObject tag = tags.getJSONObject(i);
	        				if ( tag.has("text") )
	        				{
	        					String hashtag = tag.getString("text");
	        					hashtag = hashtag.toLowerCase();
	        					// Include multiple instances
	        					int count = 1;
	        					if ( tag.has("indices") )
	        						count = tag.getJSONArray("indices").length();
	        					// Write the output
	        					context.write(new Text(hashtag), new IntWritable(count));
	        					context.getCounter("ParseHashtags", "Hashtags discovered").increment(1);
	        				}
	        			}
	        		}
	        	}
	        	//
	        	context.getCounter("ParseHashtags", "JSON objects processed").increment(1);
	        }
	        catch ( Exception ignore ) {}
	    }
		
	}
	
	/**
	 * 
	 */
	static class MyReducer extends Reducer<Text,IntWritable,Text,IntWritable>
	{
		@Override
		public void reduce(Text key, Iterable<IntWritable> values, Reducer<Text,IntWritable,Text,IntWritable>.Context context) throws IOException, InterruptedException
		{
			int sum = 0;
			for (IntWritable val : values)
				sum += val.get();
			context.write(key, new IntWritable(sum));
		}
	}
	
    /**
     * 
     */
	@Override
	public int run(String[] args) throws Exception
	{
		String inputPath = args[0];
		String outputPath = args[1];
		
		// Delete output path if it already exists
		FileSystem hdfs = FileSystem.get(conf);
		hdfs.delete(new Path(outputPath), true);
		hdfs.close();

		//
		if ( conf == null )
			conf = new Configuration();
		Job job = new Job(conf);
		job.setJobName(this.getClass().getName());
		job.setJarByClass(MyMapper.class);
		job.setMapperClass(MyMapper.class);
		job.setReducerClass(MyReducer.class);
		
		//
		job.setMapOutputKeyClass(Text.class);
		job.setMapOutputValueClass(IntWritable.class);
		
		//
		job.setInputFormatClass(TextInputFormat.class);
		FileInputFormat.setInputPaths(job, new Path(inputPath));
		FileOutputFormat.setOutputPath(job, new Path(outputPath));
		
		//
		job.waitForCompletion(true);
		
		return 0;
	}

	/**
	 * 
	 */
	public static void main(String[] args) throws Exception
	{
        ToolRunner.run(new ParseHashtags(), args);
	}

	Configuration conf = null;
		
	@Override
	public Configuration getConf()
	{
		return conf;
	}

	@Override
	public void setConf(Configuration arg0)
	{
		conf = arg0;		
	}

}

