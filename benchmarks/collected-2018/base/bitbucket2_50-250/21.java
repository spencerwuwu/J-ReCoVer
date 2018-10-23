// https://searchcode.com/api/result/134046535/

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package wmr.citations;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.*;
import org.apache.hadoop.mapreduce.Reducer.Context;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import wikiParser.mapReduce.util.KeyValueTextInputFormat;

/**
 *@author Shilad Sen
 */
public class SnapshotGenerator extends Configured implements Tool {
    static final String TIMESTAMP_KEY = "TIMESTAMP";

    /*
     * Reads output of CitationCounter
     */
    public static class MyMapper extends Mapper<Text, Text, Text, Text> {
        String FINAL_TSTAMP = null;

        @Override
        public void setup(Mapper.Context context) {
            FINAL_TSTAMP = context.getConfiguration().get(TIMESTAMP_KEY);
        }

        @Override
        public void map(Text key, Text value, Mapper.Context context) throws IOException, InterruptedException {
            String [] pair = key.toString().split("@");
            String [] tokens = value.toString().split("\t");
            context.progress();

            // Remove ending tab
            if (tokens.length == 8 && tokens[7].equals("")) {
                tokens = Arrays.copyOfRange(tokens, 0, 7);
            }
            
            if (pair.length != 2 || tokens.length != 7) {
                System.err.println("invalid key / value pair: " + key + ", " + value);
                return;
            }

            String tstamp = tokens[0];
            if (tstamp.compareTo(FINAL_TSTAMP) > 0) {
                return;     // too late
            }
            String pageId = pair[1];
            String url = tokens[4];
            int n = Integer.parseInt(tokens[6]);

            context.write(new Text(pageId), new Text(tstamp + "\t" + n + "\t" + url));
        }
    }

    public static class CountAtTime {
        public int count;
        public String tstamp;

        public CountAtTime(int count, String tstamp) {
            this.count = count;
            this.tstamp = tstamp;
        }
    }
    
    public static class MyReducer extends Reducer<Text,Text,Text,Text> {

        @Override
        protected void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
            Map<String, CountAtTime> counts = new HashMap<String, CountAtTime>();

            for (Text value : values) {
                String tokens[] = value.toString().split("\t");
                if (tokens.length != 3) {
                    System.err.println("invalid reduce value:" + value);
                    continue;
                }
                String tstamp = tokens[0];
                int n = Integer.parseInt(tokens[1]);
                String url = tokens[2];

                CountAtTime ct = counts.get(url);
                if (ct == null) {
                    counts.put(url, new CountAtTime(n, tstamp));
                } else if (tstamp.compareTo(ct.tstamp) > 0) {
                    ct.count = n;
                    ct.tstamp = tstamp;
                }
            }

            Map<String, Integer> domainCounts = new HashMap<String, Integer>();
            for (Map.Entry<String, CountAtTime> entry : counts.entrySet()) {
                String url = entry.getKey();
                int count = entry.getValue().count;
                String domain = getDomain(url);
                if (count != 0) {
                    if (domainCounts.containsKey(domain)) {
                        domainCounts.put(domain, domainCounts.get(domain) + count);
                    } else {
                        domainCounts.put(domain, count);
                    }
                }
            }

            for (Map.Entry<String, Integer> entry : domainCounts.entrySet()) {
                context.write(key, new Text(entry.getKey() + "\t" + entry.getValue()));
            }
        }

        private String getDomain(String url) {
            String[] split = url.split("/");    // http://boo.com to { "http", "", "boo.com"}
            if (split.length > 2) {
                return split[2];
            } else if (url.startsWith("wiki:")) {
                return url;
            } else {
                return url.toLowerCase();
            }
        }
    }


    public int run(String args[]) throws Exception {

        if (args.length < 3) {
            System.out.println("usage: [input output tstamp]");
            ToolRunner.printGenericCommandUsage(System.out);
            return -1;
        }

        Path inputPath = new Path(args[0]);
        Path outputPath = new Path(args[1]);

        Configuration conf = getConf();
        conf.set(TIMESTAMP_KEY, args[2]);
        Job job = new Job(conf, this.getClass().toString());

        FileInputFormat.setInputPaths(job, inputPath);
        FileOutputFormat.setOutputPath(job, outputPath);

        
        job.setJarByClass(SnapshotGenerator.class);
        job.setInputFormatClass(KeyValueTextInputFormat.class);
        job.setOutputFormatClass(TextOutputFormat.class);
        job.setMapOutputKeyClass(Text.class);
        job.setMapOutputValueClass(Text.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(Text.class);
        
        job.setMapperClass(MyMapper.class);
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
        int res = ToolRunner.run(new SnapshotGenerator(), args);
        System.exit(res);
    }
}

