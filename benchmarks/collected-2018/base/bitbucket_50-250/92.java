// https://searchcode.com/api/result/123156376/

package com.esh.hadoopfun;

import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;


public class ShingleTableGenerator {
    public static class STMapper
            extends Mapper <LongWritable, Text, Text, IntPairWritable> {
            private IntPairWritable bucketPiece;
            private Text trigram;
            private String cleanToken(String token){
                return token.toLowerCase().replaceAll("\\W+", "");
            }
            private List <Pair <String, Integer>> generateTrigrams(String document) {
                String [] tokens = document.trim().split("\\s+");
                for (int ii = 0; ii < tokens.length; ii ++ ) {
                    tokens[ii] = cleanToken(tokens[ii]);
                }
                List <Pair <String, Integer>> trigrams = new ArrayList
                                    <Pair <String, Integer>> ();
                Pair <String, Integer> currPair;
                for (int ii = 0; ii < tokens.length - 2; ii ++) {
                    String currTrigram = tokens[ii] + ' ' +
                                        tokens[ii+1] + ' ' +
                                         tokens[ii+2];
                    currPair = new Pair <String, Integer> (currTrigram, ii);
                    trigrams.add(currPair);
                }
                return trigrams;
            }
            @Override public void setup(Context context)
                throws IOException, InterruptedException {
                super.setup(context);
                bucketPiece = new IntPairWritable();
                trigram = new Text();
            }
            @Override public void map(LongWritable key, Text value, Context context)
                throws IOException, InterruptedException {
                String [] tokens = value.toString().split("\t", 2);
                int docID = Integer.parseInt(tokens[0]);
                List <Pair <String, Integer> > trigramPairs =
                    generateTrigrams(tokens[1]);
                for (Pair <String, Integer> t : trigramPairs) {
                    trigram.set(t.getFirst().trim());
                    bucketPiece.set(docID, t.getSecond());
                    context.write(trigram, bucketPiece);
                }
            }
    }
    public static class STReducer
            extends Reducer <Text, IntPairWritable, Text, Text> {
            private Text shingle;
            @Override public void setup(Context context)
                throws IOException, InterruptedException {
                super.setup(context);
                shingle = new Text();
            }
            @Override public void reduce(Text key, Iterable<IntPairWritable> values, Context context)
                throws IOException, InterruptedException {
                StringBuilder shingleBuilder = new StringBuilder();
                for (IntPairWritable value : values) {
                    shingleBuilder.append(value.toString()).append("\t");
                }
                shingle.set(shingleBuilder.toString().trim());
                context.write(key, shingle);
            }
    }
    public static void main (String [] args)
    throws IOException, InterruptedException, ClassNotFoundException {
        Configuration conf = new Configuration();
        conf.setBoolean("mapred.output.compress", false);
        Job job = new Job(conf,"Shingle Table Generator");
        job.setJarByClass(ShingleTableGenerator.class);
        job.setMapperClass(STMapper.class);
        job.setReducerClass(STReducer.class);
        job.setMapOutputKeyClass(Text.class);
        job.setMapOutputValueClass(IntPairWritable.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(Text.class);
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

