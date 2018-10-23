// https://searchcode.com/api/result/70731259/

import java.io.IOException;
import java.util.*;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.conf.*;
import org.apache.hadoop.io.*;
import org.apache.hadoop.mapreduce.*;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;


public class MinMaxCount{

    public static class MinMaxCountMapper extends
	Mapper <Object, Text, Text, MinMaxCountTuple>{
	    private Text airlineID = new Text();
	    private MinMaxCountTuple outTuple = new MinMaxCountTuple();

	    public void map(Object key, Text value, Context context)
		throws IOException, InterruptedException{
		    String line = value.toString();
		    String[] fields = line.split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)");
		    airlineID.set(fields[0]);
		    Integer delay = Integer.valueOf(fields[1]);
		    outTuple.setMin(delay);
		    outTuple.setMax(delay);
		    if (delay>0) outTuple.setCount(1);
		    context.write(airlineID,outTuple);
		}
	}

    public static class MinMaxCountReducer extends
	Reducer<Text, MinMaxCountTuple, Text, MinMaxCountTuple>{
	    private MinMaxCountTuple result=new MinMaxCountTuple();
	    public void reduce (Text key, Iterable<MinMaxCountTuple> values, Context context) throws IOException, InterruptedException {
		result.setMin(null);
		result.setMax(null);
		result.setCount(0);
		int sum=0;

		for (MinMaxCountTuple val : values){
		    if(result.getMin()==null||val.getMin()<=result.getMin()){
			result.setMin(val.getMin());
		    }

		    if(result.getMax()==null || val.getMax()>result.getMax()){
			result.setMax(val.getMax());
		    }

		    sum+=val.getCount();
		}

		result.setCount(sum);
		context.write(key, result);
	    }
	}

    public static void main (String [] args) throws Exception {
	Configuration conf = new Configuration();
	Job job = new Job (conf, "MinMaxCount");
	job.setJarByClass(MinMaxCount.class);
	job.setOutputKeyClass(Text.class);
	job.setOutputValueClass(MinMaxCountTuple.class);

	job.setMapperClass(MinMaxCountMapper.class);
	job.setReducerClass(MinMaxCountReducer.class);

	FileInputFormat.addInputPath(job, new Path(args[0]));
	FileOutputFormat.setOutputPath(job, new Path(args[1]));
	job.waitForCompletion(true);
    }
}



