// https://searchcode.com/api/result/94159083/

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.*;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

import java.io.IOException;
import java.util.Iterator;
import java.util.StringTokenizer;

/**
 * Created by IntelliJ IDEA.
 * User: shaines
 * Date: 12/9/12
 * Time: 9:25 PM
 * To change this template use File | Settings | File Templates.
 */
public class WordCount extends Configured implements Tool {

    public static class MapClass extends MapReduceBase
            implements Mapper<LongWritable, Text, Text, IntWritable>
    {
        private Text word = new Text();
        private final static IntWritable one = new IntWritable( 1 );

        public void map( LongWritable key, // Offset into the file
                         Text value,
                         OutputCollector<Text, IntWritable> output,
                         Reporter reporter) throws IOException
        {
            // Get the value as a String
            String text = value.toString().toLowerCase();

            // Replace all non-characters
            text = text.replaceAll( "'", "" );
            text = text.replaceAll( "[^a-zA-Z]", " " );

            // Iterate over all of the words in the string
            StringTokenizer st = new StringTokenizer( text );
            while( st.hasMoreTokens() )
            {
                // Get the next token and set it as the text for our "word" variable
                word.set( st.nextToken() );

                // Output this word as the key and 1 as the value
                output.collect( word, one );
            }
        }
    }

    public static class Reduce extends MapReduceBase
            implements Reducer<Text, IntWritable, Text, IntWritable>
    {
        public void reduce( Text key, Iterator<IntWritable> values,
                            OutputCollector<Text, IntWritable> output,
                            Reporter reporter) throws IOException
        {
            // Iterate over all of the values (counts of occurrences of this word)
            int count = 0;
            while( values.hasNext() )
            {
                // Add the value to our count
                count += values.next().get();
            }

            // Output the word with its count (wrapped in an IntWritable)
            output.collect( key, new IntWritable( count ) );
        }
    }


    public int run(String[] args) throws Exception
    {
        // Create a configuration
        Configuration conf = getConf();

        // Create a job from the default configuration that will use the WordCount class
        JobConf job = new JobConf( conf, WordCount.class );

        // Define our input path as the first command line argument and our output path as the second
        Path in = new Path( args[0] );
        Path out = new Path( args[1] );

        // Create File Input/Output formats for these paths (in the job)
        FileInputFormat.setInputPaths( job, in );
        FileOutputFormat.setOutputPath( job, out );

        // Configure the job: name, mapper, reducer, and combiner
        job.setJobName( "WordCount" );
        job.setMapperClass( MapClass.class );
        job.setReducerClass( Reduce.class );
        job.setCombinerClass( Reduce.class );

        // Configure the output
        job.setOutputFormat( TextOutputFormat.class );
        job.setOutputKeyClass( Text.class );
        job.setOutputValueClass( IntWritable.class );

        // Run the job
        JobClient.runJob(job);
        return 0;
    }

    public static void main(String[] args) throws Exception
    {
        // Start the WordCount MapReduce application
        int res = ToolRunner.run( new Configuration(),
                new WordCount(),
                args );
        System.exit( res );
    }
}
