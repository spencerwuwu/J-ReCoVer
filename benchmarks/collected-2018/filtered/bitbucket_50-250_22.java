// https://searchcode.com/api/result/102256889/

package com.virkaz.star.hadoopio;

import java.io.IOException;
import java.util.StringTokenizer;
import  com.virkaz.star.hadoopio.BinaryVectorInputFormat;

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
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.SequenceFileOutputFormat;
import org.apache.hadoop.util.GenericOptionsParser;

public class VectorOperations {
 private static final Log logger = LogFactory.getLog(VectorSumMapper.class);
	private static final int doubleOffset = 8;
	private static final int numBytes= 256;
	private static final double recordLength= numBytes*numBytes*numBytes*5;
	private static final double rowSize = Math.sqrt(recordLength);
    private static final int M = (int)(rowSize/doubleOffset);



  public static class VectorSumMapper 
  
       extends Mapper<NullWritable, BytesWritable, IntWritable, DoubleWritable>{
    

		/*
		public void map(NullWritable key, BytesWritable data,
				OutputCollector<NullWritable, BytesWritable> output, Reporter reporter)
		throws IOException {
			*/
		public void map(NullWritable key, BytesWritable data, Context context
        ) throws IOException, InterruptedException {
			// Process the 
			double [] dataMatrix= new double[VectorOperations.M];
			byte[] bArray= data.getBytes();
			double dValue;
			StringBuffer sb;
			System.out.println("Running the job");
			// logger.info("Byte length of the BytesWritable data "+bArray.length);
			double result= 0;
			//for(int i = 0;i<VectorOperations.M;i++){
			for(int i = 0;i<1;i++){
				// run test here
				//for (int j= 0;j<3;j++){
				//sb= new StringBuffer();	
				dValue= EndianUtils.readSwappedDouble(bArray,i*doubleOffset);
				// sb.append("Value at ").append(i).append(" is ").append(dValue);
				// logger.info(sb.toString());
				dataMatrix[i]= dValue;
				// System.out.format("Value of element %d inside map read is %f%n",i,dValue);
				// Assert.assertTrue(Math.abs(dValue - correctValues[i]) < 0.00001);
				result = Math.max(dValue, result);
				}
			
			// System.out.println("First value is "+result);
			//double result= com.virkaz.star.hadoopio.MatrixUtils.mRank(dataMatrix);
	        //      System.out.println("First value is "+inputVal);

			DoubleWritable bmr= new DoubleWritable();
			//byte[] dataArray = new byte[8];
			//EndianUtils.writeSwappedDouble(dataArray, 0, result);
			bmr.set(result);
			//bmr.set(result);
			// System.out.println("Result of map is "+result);
			context.write(new IntWritable(1), bmr);
			//output.collect(key, bmr);
			
			
		}
  }
  
  public static class VectorSumReducer 
       extends Reducer<IntWritable,DoubleWritable,IntWritable,DoubleWritable> {
    private DoubleWritable result = new DoubleWritable();

    public void reduce(IntWritable key, Iterable<DoubleWritable> values, 
                       Context context
                       ) throws IOException, InterruptedException {
      double sum = 0.0;
      //byte[] dataArray = new byte[8];
      for (DoubleWritable val : values) {
    	  
    	  sum = Math.max(sum,val.get());
        //val.get();
      }
      System.out.println("Result of reduce is "+sum);
      //EndianUtils.writeSwappedDouble(dataArray, 0, sum);
      result.set(sum);
      //result.set(sum);
      context.write(new IntWritable(1), result);
    }
  }

  @SuppressWarnings("deprecation")
public static void main(String[] args) throws Exception {
    Configuration conf = new Configuration();
    //conf.
    String[] otherArgs = new GenericOptionsParser(conf, args).getRemainingArgs();
    if (otherArgs.length != 2) {
      System.err.println("Usage: vectorOps <in> <out>");
      System.exit(2);
    }
    conf.setInt("dfs.block.size", 999950884);
    Job job = new Job(conf, "vector multiply");
    job.setJarByClass(VectorOperations.class);
    job.setMapperClass(VectorSumMapper.class);
    job.setInputFormatClass(BinaryVectorInputFormat.class);
    //job.setOutputFormatClass(cls)
    //job.setOutputFormatClass(SequenceFileOutputFormat.class);
    job.setCombinerClass(VectorSumReducer.class);
    job.setReducerClass(VectorSumReducer.class);
    job.setOutputKeyClass(IntWritable.class);
    //job.setOutputValueClass(BytesWritable.class);
    job.setOutputValueClass(DoubleWritable.class);
    //BinaryMatrixInputFormat.addInputPath(job, new Path(otherArgs[0]));
    BinaryVectorInputFormat.addInputPath(job, new Path(otherArgs[0]));
    //FileOutputFormat.setOutputPath(job, new Path(otherArgs[1]));
    SequenceFileOutputFormat.setOutputPath(job, new Path(otherArgs[1]));
    //job.submit();
    //job.
    System.exit(job.waitForCompletion(true) ? 0 : 1);
  }
}

