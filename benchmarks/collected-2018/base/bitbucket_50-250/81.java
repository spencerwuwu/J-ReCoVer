// https://searchcode.com/api/result/123156378/

package com.esh.hadoopfun;

import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import org.apache.hadoop.conf.Configuration;
import java.util.Set;
import java.util.HashSet;
import java.util.Collections;
import java.util.Map;
import java.util.HashMap;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.Partitioner;
public class SoupedInvertedIndex {
    static class IIMap extends Mapper <LongWritable, Text, TextPairWritable, NullWritable> {
        private Text currTrigram;
        private Text currToken;
        private NullWritable dummyValue;
        private TextPairWritable pairKey;
        private Map <String, Set <String> > buffer;
        @Override public void setup(Context context)  throws IOException, InterruptedException{
            super.setup(context);
            currTrigram = new Text();
            currToken = new Text();
            dummyValue = NullWritable.get();
            pairKey = new TextPairWritable();
            buffer = new HashMap <String, Set <String> > ();
        }
            private String cleanToken(String token){
                return token.toLowerCase().replaceAll("\\W+", "");
            }
            private List <String>  generateTrigrams(String token){
                token = '#' + token + '#';
                List <String> trigrams = new ArrayList <String> ();
                for (int ii = 0; ii < token.length() - 2; ii ++ ) {
                    trigrams.add(token.substring(ii, ii + 3));
                }
                return trigrams;
            }
        @Override public void map (LongWritable key, Text value, Context context)
                throws IOException, InterruptedException {
            String [] tokens = value.toString().split("\\s+");
            System.out.println("In Map");
            for (String token: tokens) {
                String cleanedToken = cleanToken(token);
                List <String> trigrams = generateTrigrams(cleanedToken);
                for (String trigram: trigrams) {
                    if(buffer.containsKey(trigram)) {
                        Set <String> currTokens = buffer.get(trigram);
                        currTokens.add(cleanedToken);
                        buffer.put(trigram, currTokens);
                    }
                    else {
                        Set <String> currTokens = new HashSet <String> ();
                        currTokens.add(cleanedToken);
                        buffer.put(trigram, currTokens);
                    }
                }
            }
        }
        @Override public void cleanup (Context context)
         throws IOException, InterruptedException {
         System.out.println("In Cleanup");
            for(Map.Entry <String, Set <String> > e: buffer.entrySet()) {
                currTrigram.set(e.getKey());
                for (String outToken: e.getValue()) {
                    currToken.set(outToken);
                    pairKey.set(currTrigram,currToken);
                    context.write(pairKey, dummyValue);
                }
            }
            super.cleanup(context);
        }
    }
    static class KeyPartitioner extends Partitioner<TextPairWritable, NullWritable> {

        @Override
            public int getPartition(TextPairWritable key, NullWritable value, int numPartitions) {
                return (key.getFirst().hashCode()  & Integer.MAX_VALUE) % numPartitions;
            }
    }

    static class IIReducer extends Reducer <TextPairWritable, NullWritable, Text, Text> {
        private Map <String, List <String> > buffer;
        private Text currWords;
        private Text currTrigram;
        @Override public void setup(Context context) throws IOException, InterruptedException{
            super.setup(context);
            buffer = new HashMap <String, List <String> > ();
            currWords = new Text();
            currTrigram = new Text();
        }
        @Override public void reduce (TextPairWritable key, Iterable <NullWritable> values, Context context)  throws IOException, InterruptedException{
            String currTrigram = key.getFirst().toString();
            String currToken = key.getSecond().toString();
            if (buffer.containsKey(currTrigram)) {
                List <String> currTokens = buffer.get(currTrigram);
                currTokens.add(currToken);
                buffer.put(currTrigram, currTokens);
            }
            else {
                List <String> currTokens = new ArrayList <String> ();
                currTokens.add(currToken);
                buffer.put(currTrigram, currTokens);
            }

        }
        @Override public void cleanup(Context context) throws IOException, InterruptedException{
            for (Map.Entry <String, List <String> > e: buffer.entrySet()) {
                currTrigram.set(e.getKey());
                StringBuilder wordListBuilder = new StringBuilder();
                for (String currToken: e.getValue()) {
                    wordListBuilder.append(currToken).append("\t");
                }
                currWords.set(wordListBuilder.toString().trim());
                context.write(currTrigram, currWords);
            }
            super.cleanup(context);
        }
    }
    public static void main (String [] args) throws IOException, InterruptedException, ClassNotFoundException{
        Configuration conf = new Configuration();
        Job job = new Job(conf,"Simple Inverted Index");
        job.setJarByClass(SoupedInvertedIndex.class);
        job.setMapperClass(IIMap.class);
        job.setReducerClass(IIReducer.class);
        job.setMapOutputKeyClass(TextPairWritable.class);
        job.setMapOutputValueClass(NullWritable.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(Text.class);
        job.setPartitionerClass(KeyPartitioner.class);
        FileSystem fs = FileSystem.get(conf);
        Path inputPath = new Path(args[0]);
        Path outputPath = new Path(args[1]);
        if(fs.exists(outputPath) ){
            fs.delete(outputPath);
        }
        FileInputFormat.addInputPath(job, inputPath);
        FileOutputFormat.setOutputPath(job, new Path(args[1]));
        try{
            job.setNumReduceTasks(Integer.parseInt(args[2]));
        }catch(NumberFormatException ne) {
            System.err.println("Illegal number");
        }
        if (job.waitForCompletion(true)){
            System.exit(0);
        }
        else {
            System.exit(1);
        }
    }
}

