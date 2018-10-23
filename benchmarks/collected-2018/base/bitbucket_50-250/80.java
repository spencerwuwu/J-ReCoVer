// https://searchcode.com/api/result/123156372/

package com.esh.hadoopfun;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HashMap;
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
import org.apache.hadoop.mapreduce.Partitioner;

public class SequenceGeneratorJob {
    public static class SQMapper extends
        Mapper <LongWritable, Text, IntPairWritable, Text> {
            private IntPairWritable bucketPiece;
            private Text bucket;
            private final Integer anarchismDocID = new Integer(10);
            @Override public void setup(Context context)
                throws IOException, InterruptedException{
                bucketPiece = new IntPairWritable();
                bucket = new Text();
            }
            @Override public void
                map(LongWritable key, Text value, Context context)
                throws IOException, InterruptedException{
                String [] shingleInfo = value.toString().split("\t", 2);
                String bucketStr = shingleInfo[1];
                BucketProcessor bp = new BucketProcessor(bucketStr);
                if(bp.getDocIDs().contains(anarchismDocID)) {
                    List <Integer> shinglePositions =
                        bp.getPositions(anarchismDocID);
                    bucket.set(bucketStr);
                    for (Integer shinglePosition: shinglePositions) {
                        System.out.println(shinglePosition);
                        bucketPiece.set(anarchismDocID, shinglePosition);
                        context.write(bucketPiece, bucket);
                    }
                }
                }
        }
    public static class Sequence {
        private int index;
        private int length;
        private int otherID;
        private int otherIndex;
        public Sequence(int index, int length, int otherID, int otherIndex) {
            this.index = index;
            this.length = length;
            this.otherID = otherID;
            this.otherIndex = otherIndex;
        }
        @Override public String toString(){
            // TODO Convert from primitive to String without this hack
            String index = Integer.toString(this.index);
            String length = Integer.toString(this.length);
            String otherID = Integer.toString(this.otherID);
            String otherIndex = Integer.toString(this.otherIndex);
            return index + ", " + length + " (" + otherID + ", " + otherIndex + ")";
        }
        public int getOtherID() {
            return otherID;
        }
        public int getOtherIndex() {
            return otherIndex;
        }
        public int getLength() {
            return length;
        }
        public void setLength(int length) {
            this.length = length;
        }
        @Override public int hashCode(){
            int hash = 1;
            hash = hash * 17 + index;
            hash = hash * 13 + length;
            hash = hash * 31 + otherID;
            hash = hash * 11 + otherIndex;
            return hash;
        }
        @Override public boolean equals(Object obj){
            if(obj == this){
                return true;
            }
            if(obj == null || obj.getClass() != this.getClass()){
                return false;
            }
            Sequence other = (Sequence) obj;
            return (index == other.index) && (length == other.length) && (otherID == other.otherID) && (otherIndex == other.otherIndex);
        }

    }
    public static class SequenceGenerator {
        Set <Sequence> activeSequences;
        Set <Sequence> sequences;
        private int index;
        private int sourceID;
        public SequenceGenerator (int sourceID){
            activeSequences = new HashSet <Sequence> ();
            sequences = new HashSet <Sequence> ();
            index = 0;
            sourceID = sourceID;
        }

        public void build(List <Pair <Integer, Integer>> bucketList) {
            Set <Pair <Integer, Integer>> bucket = new HashSet <Pair < Integer, Integer> > (bucketList);
            for (Sequence s : activeSequences) {
                int otherID = s.getOtherID();
                int otherIndex = s.getOtherIndex();
                Pair <Integer, Integer> newPair = new Pair <Integer, Integer>
                    (otherID, otherIndex + s.getLength());
                if (bucket.contains(newPair)) {
                    s.setLength(s.getLength() + 1);
                    bucket.remove(newPair);
                }
                else {
                    sequences.add(s);
                }
            }
            for (Sequence s : sequences) {
                if (activeSequences.contains(s)) {
                    activeSequences.remove(s);
                }
            }
            for (Pair <Integer, Integer> bucketPiece: bucket) {
                if (!(bucketPiece.getFirst().equals(sourceID))) {
                    Sequence a = new Sequence(index, 1, bucketPiece.getFirst(), bucketPiece.getSecond());
                    activeSequences.add(a);
                }
            }
            ++index;
        }
        public Set<Sequence> getSequences() {
            return this.sequences;
        }
    }
    public static class SQReducer extends
        Reducer <IntPairWritable, Text, Text, Text> {
            private SequenceGenerator sg;
            private final Integer anarchismDocID = new Integer(10);
            private Text sourceDocID;
            private Text concatenatedSequences;
            @Override public void setup(Context context)
                throws IOException, InterruptedException{
                super.setup(context);
                sourceDocID = new Text(anarchismDocID.toString());
                concatenatedSequences = new Text();
                sg = new SequenceGenerator(anarchismDocID);
            }
            @Override public void reduce(IntPairWritable key, Iterable <Text> values, Context context) throws IOException, InterruptedException {
                String bucketStr = values.iterator().next().toString();
                BucketProcessor bp = new BucketProcessor(bucketStr);
                List <Pair <Integer, Integer>> bucket = bp.getPairs();
                sg.build(bucket);
            }
            @Override public void cleanup (Context context)
                throws IOException, InterruptedException {
                Set <Sequence> sequences = sg.getSequences();
                StringBuilder concatenatedSeqBuilder = new StringBuilder();
                for (Sequence s: sequences) {
                    concatenatedSeqBuilder.append("[")
                        .append(s.toString())
                        .append("]")
                        .append("\t");
                }
                concatenatedSequences.set(concatenatedSeqBuilder.toString().trim());
                context.write(sourceDocID, concatenatedSequences);
            }
        }
    public static class SQPartitioner extends  Partitioner <IntPairWritable, Text> {
        @Override
            public int getPartition(IntPairWritable key, Text value, int numPartitions) {
                return (key.getFirst().hashCode()  & Integer.MAX_VALUE) % numPartitions;
            }
    }

    public static void main (String [] args)
        throws IOException, InterruptedException, ClassNotFoundException {
        Configuration conf = new Configuration();
        conf.setBoolean("mapred.output.compress", false);
        Job job = new Job(conf,"Sequence Generator");
        job.setJarByClass(SequenceGeneratorJob.class);
        job.setMapperClass(SQMapper.class);
        job.setReducerClass(SQReducer.class);
        job.setMapOutputKeyClass(IntPairWritable.class);
        job.setMapOutputValueClass(Text.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(Text.class);
        job.setPartitionerClass(SQPartitioner.class);
        FileSystem fs = FileSystem.get(conf);
        Path inputPath = new Path(args[0]);
        Path outputPath = new Path(args[1]);
        if(fs.exists(outputPath) ){
            fs.delete(outputPath);
        }
        FileInputFormat.addInputPath(job, inputPath);
        FileOutputFormat.setOutputPath(job, new Path(args[1]));
        if (job.waitForCompletion(true)){
            System.exit(0);
        }
        else {
            System.exit(1);
        }
    }
}

