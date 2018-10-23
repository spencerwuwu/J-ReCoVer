// https://searchcode.com/api/result/99227175/

import java.io.IOException;
import java.util.Iterator;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.lib.input.KeyValueTextInputFormat;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author zengzhaozheng
 *
 * :mapreduce
 * :
 * --------------------------------
 * sort1,1
 * sort2,3
 * sort2,77
 * sort2,54
 * sort1,2
 * sort6,22
 * sort6,221
 * sort6,20
 * ------------------------------
 * sort1 1,2
 * sort2 3,54,77
 * sort6 20,22,221
 */
public class Secondsort extends Configured  implements Tool {
    private static final Logger logger = LoggerFactory.getLogger(Secondsort.class);
    public static class SortMapper extends Mapper<Text, Text, CombinationKey, IntWritable> {
    //---------------------------------------------------------
        /**
         * ,map
         * ,,mapreduce,
         * map,,map1
         * ,map4
         * (4*1,javagc,
         * ,GC),map,
         * 4
         */
        CombinationKey combinationKey = new CombinationKey();
        Text sortName = new Text();
        IntWritable score = new IntWritable();
        String[] inputString = null;
    //---------------------------------------------------------
        @Override
        protected void map(Text key, Text value, Context context)
                throws IOException, InterruptedException {
            //logger.info("---------enter map function flag---------");
            //
            if(key == null || value == null || key.toString().equals("")
                    || value.equals("")){
                return;
            }
            sortName.set(key.toString());
            score.set(Integer.parseInt(value.toString()));
            combinationKey.setFirstKey(sortName);
            combinationKey.setSecondKey(score);
            //map
            context.write(combinationKey, score);
            //logger.info("---------out map function flag---------");
        }
    }
    public static class SortReducer extends
    Reducer<CombinationKey, IntWritable, Text, Text> {
        StringBuffer sb = new StringBuffer();
        Text sore = new Text();
        /**
         * reduce:reduce
         * reduce,?:
         * eg:
         * {{sort1,{1,2}},{sort2,{3,54,77}},{sort6,{20,22,221}}}
         * ,{sort1,{1,2}}
         * {sort2,{3,54,77}}{sort6,{20,22,221}}
         */
        @Override
        protected void reduce(CombinationKey key,
                Iterable<IntWritable> value, Context context)
                throws IOException, InterruptedException {
            sb.delete(0, sb.length());//
            Iterator<IntWritable> it = value.iterator();
                                                                                                                                                                                          
            while(it.hasNext()){
                sb.append(it.next()+",");
            }
            //
            if(sb.length()>0){
                sb.deleteCharAt(sb.length()-1);
            }
            sore.set(sb.toString());
            context.write(key.getFirstKey(),sore);
            //logger.info("---------enter reduce function flag---------");
            //logger.info("reduce Input data:{["+key.getFirstKey()+","+
            //key.getSecondKey()+"],["+sore+"]}");
            //logger.info("---------out reduce function flag---------");
        }
    }
    @Override
    public int run(String[] args) throws Exception {
        Configuration conf=getConf(); //
        Job job=new Job(conf,"SoreSort");
        job.setJarByClass(Secondsort.class);
                                                                                                                                                                                      
        FileInputFormat.addInputPath(job, new Path(args[0])); //map
        FileOutputFormat.setOutputPath(job, new Path(args[1])); //reduce
                                                                                                                                                                                                                                                                                                                           
        job.setMapperClass(SortMapper.class);
        job.setReducerClass(SortReducer.class);
                                                                                                                                                                                      
        job.setPartitionerClass(DefinedPartition.class); //
        job.setNumReduceTasks(20);
                                                                                                                                                                                                                                                                                                                           
        job.setGroupingComparatorClass(DefinedGroupSort.class); //
        job.setSortComparatorClass(DefinedComparator.class); //
                                                                                                                                                                                     
        job.setInputFormatClass(KeyValueTextInputFormat.class); //
        job.setOutputFormatClass(TextOutputFormat.class);//output
                                                                                                                                                                                      
        //mapkeyvalue
        job.setMapOutputKeyClass(CombinationKey.class);
        job.setMapOutputValueClass(IntWritable.class);
                                                                                                                                                                                      
        //reducekeyvalue
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(Text.class);
        job.waitForCompletion(true);
        return job.isSuccessful()?0:1;
    }
                                                                                                                                                                                  
    public static void main(String[] args) {
        try {
            int returnCode =  ToolRunner.run(new Secondsort(),args);
            System.exit(returnCode);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
                                                                                                                                                                                      
    }
}
