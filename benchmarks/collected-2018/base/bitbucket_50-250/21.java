// https://searchcode.com/api/result/102256885/

package com.virkaz.star.hadoopio;

import java.io.IOException;
import java.util.StringTokenizer;

import org.apache.commons.io.EndianUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.OutputCollector;
import org.apache.hadoop.mapred.Reporter;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.Mapper.Context;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.FileSplit;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.SequenceFileOutputFormat;
import org.apache.hadoop.util.GenericOptionsParser;

/**
 * Input to the mapper is the data file to process. Passing the file as input is just to
 * make sure that it is invoking the vm on the right node. 
 * Assume that the directory structure is 
 * VIRTUAL DISK
 * hduser 
 *  Events
 *   EventFile1
 *   EventFile2
 * Global Cache
 *  VM
 *   VM1
 *   VM2
 * Local Cache
 *  vm-launcher-script.sh
 * Production
 *  DS1
 *  DS2
 *
 * Invoke with -input Events -vm VM1 -output Production
 * we want the Production and Events to be on separate filesystems if that is possible.
 * invokes the virtual machine VM1 and then the VM is mounted with the local disk and performs
 * analysis on the file.
 */
public class InvokeVM {

  public static class ShellOperationMapper
       extends Mapper<NullWritable, BytesWritable,Text, Text>{
    
		private static final Log logger = LogFactory.getLog(InvokeVM.class);
		/*
		public void map(NullWritable key, BytesWritable data,
				OutputCollector<NullWritable, BytesWritable> output, Reporter reporter)
		throws IOException {
			*/
		public void map(NullWritable key, BytesWritable data, Context context
        ) throws IOException, InterruptedException {
			FileSplit fs = (FileSplit)context.getInputSplit();
			System.out.println("Running the job");
			String fileName= fs.getPath().toString().split(":")[1];
			logger.info("The file being processed is "+fileName);
			// Everything happens here.
			Process p = Runtime.getRuntime().exec("invokeVM.sh vm1 analysis_command "+fileName);
			byte[] b= new byte[10000];
			p.getInputStream().read(b);
			String outputString= new String (b);
			System.out.println("Result of map is "+outputString);
			//context.write(new Text(fs.toString()), new DoubleWritable(result));
			context.write(new Text(fs.toString()), new Text(outputString));
			//output.collect(key, bmr);
			
			
		}
  }
  
  public static class ShellOperationReducer 
       extends Reducer<Text,Text,Text,Text> {
    //private DoubleWritable result = new DoubleWritable();

	private Text result = new Text();
    public void reduce(Text key, Iterable<Text> values, 
                       Context context
                       ) throws IOException, InterruptedException {
      //double sum = 0.0;
    	StringBuffer sum= new StringBuffer();
      byte[] dataArray = new byte[8];
      for (Text val : values) {
    	  
    	  //sum += EndianUtils.readSwappedDouble(val.getBytes(), 0);
    	  // sum += val.get();
    	  sum.append(val);
        //val.get();
      }
      System.out.println("Result of reduce is "+sum);
      //EndianUtils.writeSwappedDouble(dataArray, 0, sum);
      result.set(sum.toString());
      //result.set(sum);
      context.write(key, result);
    }
  }

  @SuppressWarnings("deprecation")
public static void main(String[] args) throws Exception {
    Configuration conf = new Configuration();
    String[] otherArgs = new GenericOptionsParser(conf, args).getRemainingArgs();
    if (otherArgs.length != 2) {
      System.err.println("Usage: matrixOps <in> <out>");
      System.exit(2);
    }
    Job job = new Job(conf, "matrix multiply");
    job.setJarByClass(ShellOperations.class);
    job.setMapperClass(ShellOperationMapper.class);
    job.setInputFormatClass(BinaryMatrixInputFormat.class);
    //job.setOutputFormatClass(cls)
    //job.setOutputFormatClass(SequenceFileOutputFormat.class);
    //job.setOutputFormatClass(FileOutputFormat.class);
    job.setCombinerClass(ShellOperationReducer.class);
    job.setReducerClass(ShellOperationReducer.class);
    job.setOutputKeyClass(Text.class);
    //job.setOutputValueClass(BytesWritable.class);
    //job.setOutputValueClass(DoubleWritable.class);
    job.setOutputValueClass(Text.class);
    //BinaryMatrixInputFormat.addInputPath(job, new Path(otherArgs[0]));
    BinaryMatrixInputFormat.addInputPath(job, new Path(otherArgs[0]));
    FileOutputFormat.setOutputPath(job, new Path(otherArgs[1]));
    //SequenceFileOutputFormat.setOutputPath(job, new Path(otherArgs[1]));
    //job.submit();
    System.exit(job.waitForCompletion(true) ? 0 : 1);
  }
}

