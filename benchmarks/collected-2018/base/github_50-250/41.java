// https://searchcode.com/api/result/105853919/

package com.dburger.hadoop.anagram;

import java.io.IOException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;

import org.apache.hadoop.fs.Path;

import org.apache.hadoop.io.ArrayWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.Writable;

import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;

import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;;

import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

public class AnagramList extends Configured implements Tool {

    public static class TextArrayWritable extends ArrayWritable {
        public TextArrayWritable() {super(Text.class);}
        public String toString() {
            return Arrays.toString(toStrings());
        }
    }

    public static class Map extends Mapper<LongWritable, Text, Text,
                TextArrayWritable> {
        private final static TextArrayWritable writable
                = new TextArrayWritable();
        private final static Writable[] writables = new Writable[1];
        private Text newKey = new Text();

        public void map(LongWritable key, Text value, Context context)
                throws IOException, InterruptedException {
            String word = value.toString();
            char[] chars = word.toCharArray();
            Arrays.sort(chars);
            newKey.set(new String(chars));
            writables[0] = new Text(word);
            writable.set(writables);
            context.write(newKey, writable);
        }
    }

    public static class Reduce extends Reducer<Text, TextArrayWritable, Text,
            TextArrayWritable> {
        public void reduce(Text key, Iterator<TextArrayWritable> values,
                Context context) throws IOException, InterruptedException {
            List<Writable> list = new ArrayList<Writable>();
            while (values.hasNext()) {
                Writable[] writables = values.next().get();
                for (Writable t: writables) {list.add(t);}
            }
            Writable[] writables = list.toArray(new Writable[0]);
            TextArrayWritable writable = new TextArrayWritable();
            writable.set(writables);
            context.write(key, writable);
        }
    }

    public int run(String[] args) throws Exception {
        Path in = new Path(args[0]);
        Path out = new Path(args[1]);

        Job job = new Job(getConf(), "Anagram Lister");

        job.setJarByClass(AnagramList.class);

        FileInputFormat.setInputPaths(job, in);
        FileOutputFormat.setOutputPath(job, out);

        job.setMapperClass(Map.class);
        job.setCombinerClass(Reduce.class);
        job.setReducerClass(Reduce.class);

        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(TextArrayWritable.class);

        return job.waitForCompletion(true) ? 0 : -1;
    }

    public static void main(String[] args) throws Exception {
        int res = ToolRunner.run(new Configuration(), new AnagramList(), args);
        System.exit(res);
    }

}

