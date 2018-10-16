// https://searchcode.com/api/result/68682372/

package com.spbsu.hadoop;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Random;
import java.util.StringTokenizer;

/**
 * User: svasilinets
 * Date: 03.04.12
 * Time: 20:50
 */
public class FirstLetterCounter {


    public static final String DELAY = "firstchar.delay";

    private static class FCharMapper extends Mapper<Object, Text, Text, IntWritable>{

        static IntWritable one = new IntWritable(1);

        static Text[] chars = new Text[Character.MAX_VALUE];
        static {
            for (char i = 0; i < Character.MAX_VALUE; i++){
                chars[i] = new Text(Character.toString(i));
            }
        }

        @Override
        protected void map(Object key, Text value, Context context) throws IOException, InterruptedException {
            StringTokenizer tokenizer = new StringTokenizer(value.toString());

            while (tokenizer.hasMoreTokens()){
                char ch = tokenizer.nextToken().charAt(0);
                context.write(chars[ch], one);
            }
        }


    }



    private static String initName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            Random r = new Random();
            int k = r.nextInt(10000);
            e.printStackTrace();
            return Integer.toString(k);
        }
    }
    static String name;

    /**
     * @return Machine name,  if some error occurs it will return some random id for machine.
     *         This id isn't stable (For different jobs it will be differrent);
     */
    static String getMachineName() {
        if (name == null) {
            name = initName();
        }
        return name;
    }

    private static int  MOD = 250;
    private static int sleep = 10000;
    static int j = 0;

    private static final String WORKING_DIR = "statistic";

    private static final String COMPARISON_DIR = "comparison";

    private static int k = 0;
    private final static int KEY_NUMBER = 500000;
    private final static int DEFAULT_DELAY = 30;


    private static class OneReducer extends Reducer<Text, IntWritable, Text, IntWritable>{
        @Override
        protected void reduce(Text key, Iterable<IntWritable> values, Context context) throws IOException, InterruptedException {
            int delay = context.getConfiguration().getInt(DELAY, DEFAULT_DELAY);
            int sum = 0;
            for (IntWritable i: values){
                sum += i.get();
                if (k * KEY_NUMBER < j){
                    Thread.sleep(delay * 1000);
                    k++;
                }
                j+=i.get();
                
            }
//            Thread.sleep(3 + sum / delay);
            context.write(key, new IntWritable(sum));
        }

        @Override
        protected void cleanup(Context context) throws IOException, InterruptedException {
            super.cleanup(context);
            FileSystem fs = FileSystem.get(context.getConfiguration());
            
            Path p = new Path(new Path(WORKING_DIR, context.getJobName()), COMPARISON_DIR);
            Path w = new Path(p, context.getJobID().toString());
            FSDataOutputStream out = fs.create(new Path(new Path(w, "a"), getMachineName()));
            out.writeInt(j);
            out.close();

        }
    }
    
    
    public static void main(String[] args) throws IOException, ClassNotFoundException, InterruptedException {
        Configuration conf = new Configuration();
        conf.setInt(DELAY, Integer.parseInt(args[2]));
        Job job  = new Job(conf, "firstchar");
        sleep = Integer.parseInt(args[2]);
        System.out.println(MOD + " " + sleep);
        job.setJarByClass(FirstLetterCounter.class);
        job.setMapperClass(FCharMapper.class);
//        job.setCombinerClass(OneReducer.class);


        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(IntWritable.class);
        job.setReducerClass(OneReducer.class);

        FileInputFormat.addInputPath(job, new Path(args[0]));
        FileOutputFormat.setOutputPath(job, new Path(args[1]));
        
        System.exit(job.waitForCompletion(true) ? 0 : 1);
    }
}

