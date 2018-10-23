// https://searchcode.com/api/result/134046552/

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package wmr.tfidf;
import java.io.IOException;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

import wikiParser.mapReduce.util.KeyValueTextInputFormat;

/**
 * Fifth step in similarity calculation.
 *
 * Example invocation:
 * hadoop jar ./wikiMiner-deps.jar edu.macademia.wikiminer.FinalDocSim
 *              /user/shilad/macademia/res/4 /user/shilad/macademia/res/5
 * 
 * @author shilad
 */
public class Step5FinalDocSim extends Configured implements Tool {

    private static class MyReducer extends Reducer<Text, Text, Text, Text> {

        @Override
        public void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
            double score = 0;
            long i = 0;

            for (Text t : values) {
                String value = t.toString();
                if (i++ % 100000 == 0) {
                    context.progress();
                }
                try {               
                    score += Double.valueOf(value);
                } catch (NumberFormatException e) {
                    System.err.println("invalid key/value pair: " + key + ", " + value);
                }
                
            }
            context.write(key, new Text(""+score));
        }
    }

    /**
     * Runs this tool.
     */
    public int run(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("usage: input output");
            ToolRunner.printGenericCommandUsage(System.out);
            return -1;
        }

        Path inputPath = new Path(args[0]);
        Path outputPath = new Path(args[1]);

        Configuration conf = getConf();

        Job job = new Job(conf, this.getClass().toString());
        FileInputFormat.setInputPaths(job, inputPath);
        FileOutputFormat.setOutputPath(job, outputPath);

        job.setJarByClass(Step5FinalDocSim.class);
        job.setInputFormatClass(KeyValueTextInputFormat.class);
        job.setOutputFormatClass(TextOutputFormat.class);
        job.setMapOutputKeyClass(Text.class);
        job.setMapOutputValueClass(Text.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(Text.class);

        job.setReducerClass(MyReducer.class);
        FileSystem hdfs = FileSystem.get(outputPath.toUri(), conf);
        if (hdfs.exists(outputPath)) {
            hdfs.delete(outputPath, true);
        }

        return job.waitForCompletion(true) ? 0 : 1;
    }

    /**
     * Dispatches command-line arguments to the tool via the
     * <code>ToolRunner</code>.
     */
    public static void main(String[] args) throws Exception {
        int res = ToolRunner.run(new Configuration(), new Step5FinalDocSim(), args);
        System.exit(res);
        return;
    }
}


