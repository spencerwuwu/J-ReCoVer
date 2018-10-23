// https://searchcode.com/api/result/134046532/

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package wmr.citations;

import java.io.IOException;
import java.util.Iterator;
import org.apache.hadoop.conf.*;
import org.apache.hadoop.fs.*;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.*;
import org.apache.hadoop.util.*;

/**
 *
 * @author Nathaniel Miller
 * 
 */
public class CiteUrlCounter extends Configured implements Tool {

    public static class MyMapper extends MapReduceBase implements Mapper<Text, Text, Text, Text>{

        public void map(Text key, Text value, OutputCollector<Text, Text> output,
                Reporter reporter) throws IOException {
            output.collect(new Text(key.toString().split("@")[0]), value);
        }
        
    }
    
    public static class MyReducer extends MapReduceBase implements Reducer<Text,Text,Text,Text> {
        
        public void reduce(Text key, Iterator<Text> values, OutputCollector<Text,Text> output, Reporter reporter) throws IOException {
            int added = 0;
            int removed = 0;
            int revisions = 0;
            int articles = 0;
            while (values.hasNext()) {
                String[] v = values.next().toString().split("\t");
                added = added + Integer.parseInt(v[0]);
                removed = removed + Integer.parseInt(v[1]);
                revisions = revisions + Integer.parseInt(v[2]);
                articles++;
            }
            output.collect(key, new Text(added + "\t" + removed + "\t" + revisions + "\t" + articles));
        }
    }

    public int run(String args[]) throws Exception {

        if (args.length < 2) {
            System.out.println("usage: [input output]");
            ToolRunner.printGenericCommandUsage(System.out);
            return -1;
        }

        Path inputPath = new Path(args[0]);
        Path outputPath = new Path(args[1]);

        JobConf job = new JobConf(getConf(), this.getClass());
        job.setJobName(this.getClass().toString());
        
        FileInputFormat.setInputPaths(job, inputPath);
        FileOutputFormat.setOutputPath(job, outputPath);

        job.setInputFormat(KeyValueTextInputFormat.class);
        job.setOutputFormat(TextOutputFormat.class);
        job.setMapOutputKeyClass(Text.class);
        job.setMapOutputValueClass(Text.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(Text.class);

        job.setMapperClass(MyMapper.class);
        job.setReducerClass(MyReducer.class);
        FileSystem hdfs = FileSystem.get(outputPath.toUri(), job);
        if (hdfs.exists(outputPath)) {
            hdfs.delete(outputPath, true);
        }

        JobClient.runJob(job);

        return 0;
    }

    /**
     * Dispatches command-line arguments to the tool via the
     * <code>ToolRunner</code>.
     */
    public static void main(String[] args) throws Exception {
        int res = ToolRunner.run(new Configuration(), new CiteUrlCounter(), args);
        System.exit(res);
        return;
    }    
}

