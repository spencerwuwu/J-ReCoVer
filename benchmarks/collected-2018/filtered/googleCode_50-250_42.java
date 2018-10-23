// https://searchcode.com/api/result/12297103/

package org.leeing.hadoop.wordcount2;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.filecache.DistributedCache;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.Reducer.Context;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.apache.hadoop.util.StringUtils;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.leeing.hadoop.util.DirectoryUtil;

/**
 * @date Apr 2, 2011
 * @author leeing
 */
public class WordCount2 extends Configured implements Tool {

    public static class Map extends Mapper<LongWritable, Text, Text, Object> {

        static enum Counters {

            INPUT_WORDS
        }
        private final static IntWritable one = new IntWritable(1);
        private Text word = new Text();
        private boolean caseSensitive = true;
        private Set<String> patternsToSkip = new HashSet<String>();
        private long numRecords = 0;
        private String inputFile;

        @Override
        public void setup(Context context) {
            Configuration conf = context.getConfiguration();
            caseSensitive = conf.getBoolean("wordcount.case.sensitive", true);
            inputFile = conf.get("mapreduce.map.input.file");
            if (conf.getBoolean("wordcount.skip.patterns", false)) {
                Path[] patternsFiles = new Path[0];

                try {
                    patternsFiles = DistributedCache.getLocalCacheFiles(conf);
                } catch (IOException ex) {
                    System.err.println("Caught exception while getting cached files:"
                            + StringUtils.stringifyException(ex));
                }

                for (Path patternsFile : patternsFiles) {
                    parseSkipFile(patternsFile);
                }
            }
        }

        private void parseSkipFile(Path patternsFile) {
            try {
                BufferedReader fis = new BufferedReader(new FileReader(patternsFile.toString()));
                String pattern = null;
                while ((pattern = fis.readLine()) != null) {
                    patternsToSkip.add(pattern);
                }
            } catch (IOException ex) {
                System.err.println("Caught exception while parsing the cached file"
                        + patternsFile + " :" + StringUtils.stringifyException(ex));
            }
        }

        @Override
        public void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
            String line = (caseSensitive) ? value.toString() : value.toString().toLowerCase();
            for (String pattern : patternsToSkip) {
                line = line.replaceAll(pattern, "");
            }

            StringTokenizer tokenizer = new StringTokenizer(line);
            while (tokenizer.hasMoreTokens()) {
                word.set(tokenizer.nextToken());
                context.write(word, one);
                context.getCounter(Counters.INPUT_WORDS).increment(1);
            }

            if ((++numRecords % 100) == 0) {
                context.setStatus("Finished processing " + numRecords + " records "
                        + "from the input file.: " + inputFile);
            }
        }
    }

    public static class Reduce extends Reducer<Text, IntWritable, Text, IntWritable> {

        @Override
        protected void reduce(Text key, Iterable<IntWritable> values, Context context) throws IOException, InterruptedException {
            int sum = 0;
            for (IntWritable val : values) {
                sum += val.get();
            }
            context.write(key, new IntWritable(sum));
        }
    }

    public int run(String[] strings) throws Exception {
        
        Job job = new Job(getConf());

        job.setJarByClass(WordCount2.class);
        job.setJobName("wordcount version 2");

        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(IntWritable.class);

        job.setMapperClass(Map.class);
        job.setCombinerClass(Reduce.class);
        job.setReducerClass(Reduce.class);
        
        job.setInputFormatClass(TextInputFormat.class);
        job.setOutputFormatClass(TextOutputFormat.class);

        //***

        String from = "hdfs://localhost:8020/user/leeing/wordcount2";
        String to = "hdfs://localhost:8020/user/leeing/wordcount2/output";

        DirectoryUtil.delete(to);
        FileInputFormat.addInputPath(job, new Path(from));
        FileOutputFormat.setOutputPath(job, new Path(to));

        boolean res = job.waitForCompletion(true);
        return res ? 0 : 1;


    }

    public static void main(String[] args) throws Exception {
        int res = ToolRunner.run(new WordCount2(), args);
        System.exit(res);
    }
}

