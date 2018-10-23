// https://searchcode.com/api/result/102256864/

package com.virkaz.star.hadoopio;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.StringTokenizer;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

import org.apache.commons.io.EndianUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.filecache.DistributedCache;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.JobClient;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.OutputCollector;
import org.apache.hadoop.mapred.Reporter;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapred.Mapper;
import org.apache.hadoop.mapred.Reducer;
import org.apache.hadoop.mapred.MapReduceBase;
import org.apache.hadoop.mapreduce.Mapper.Context;
import org.apache.hadoop.mapred.FileInputFormat;
import org.apache.hadoop.mapred.FileOutputFormat;
import org.apache.hadoop.mapred.SequenceFileOutputFormat;
import org.apache.hadoop.util.GenericOptionsParser;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.apache.hadoop.mapred.Reporter;

public class MatrixSplitRunner extends Configured implements Tool {
	
    private static final Log logger = LogFactory.getLog(MatrixSplitRunner.class);
    private static int INPUT_VECTOR_SIZE= 5;
    private static int OUTPUT_VECTOR_SIZE= 5;


  public static class MatrixSumMapper 
       extends MapReduceBase implements Mapper<NullWritable, BytesWritable, NullWritable, BytesWritable>{
		private static final int doubleOffset = 8;
		public void map(NullWritable key, BytesWritable data, 
				OutputCollector<NullWritable,BytesWritable> output,
                       Reporter reporter
        ) throws IOException {
		    byte[] bArray= data.getBytes();
		    BytesWritable bmr= new BytesWritable();
		    bmr.set(bArray, 0, 4);
		    output.collect(key, bmr);		    
	}

  }
  
    
  public static class MatrixSumReducer 
       extends MapReduceBase implements Reducer<NullWritable,BytesWritable,NullWritable,BytesWritable> {
      private BytesWritable result = new BytesWritable();

      public void reduce(NullWritable key, 
			 Iterator<BytesWritable> values, 
			 OutputCollector<NullWritable,BytesWritable> output,
			 Reporter reporter
			 ) throws IOException {
	  while (values.hasNext()) {
	      BytesWritable val= values.next();
	      result.set(val.getBytes(),0,4);
	}
	output.collect(key, result);
      }
  }

  
 public void multiply(String input, String output, Configuration conf){
     JobConf job;
     try {
	 job = new JobConf(conf);
	 job.setJobName("matrix multiply");
	 job.setJarByClass(MatrixSplitRunner.class);
	 job.setMapperClass(MatrixSumMapper.class);
	 job.setInputFormat(PipesBinaryMatrixSplitInputFormat.class);
	 //job.setOutputFormatClass(cls)
	 job.setOutputFormat(SequenceFileOutputFormat.class);
	 job.setCombinerClass(MatrixSumReducer.class);
	 job.setReducerClass(MatrixSumReducer.class);
	 job.setMapOutputKeyClass(NullWritable.class);
	 job.setMapOutputValueClass(BytesWritable.class);
	 job.setOutputKeyClass(NullWritable.class);
	 //job.setOutputValueClass(BytesWritable.class);
	 job.setOutputValueClass(BytesWritable.class);
	 //BinaryMatrixInputFormat.addInputPath(job, new Path(otherArgs[0]));
	 // I think this will 
	 logger.info("Setting input and output paths");
	 //Path inputPath = new Path(input);
	 PipesBinaryMatrixSplitInputFormat.addInputPath(job, new Path(input));
	 //FileOutputFormat.setOutputPath(job, new Path(otherArgs[1]));
	 SequenceFileOutputFormat.setOutputPath(job, new Path(output));
	 logger.info("Finished configuration");
	 //	 logger.info("Configuration is "+job.getConfiguration().toString());
	 //job.submit();
	 //JobClient.runJob((JobConf)job.getConfiguration());
	 //JobClient.runJob(arg0)
	 //	 System.exit(job.waitForCompletion(true) ? 0 : 1);	 
	 JobClient.runJob(job);
	 
     } catch (IOException e) {
	 // TODO Auto-generated catch block
	 e.printStackTrace();
     }
 }
 
 // What is the best way to configure when we need to pass in directories for input and output
  @SuppressWarnings("deprecation")
public static void main(String[] args) throws Exception {
	  
    //Configuration conf = getConf();
    int ret = ToolRunner.run(new MatrixSplitRunner(), args);
    System.exit(ret);
  }

@Override
public int run(String[] args) throws Exception {
	// TODO Auto-generated method stub
          
      Configuration conf= getConf();
      String[] otherArgs = new GenericOptionsParser(conf, args).getRemainingArgs();
      if (otherArgs.length != 4) {
        System.err.println("Usage: matrixOps <row> <col> <in> <out>");
        System.exit(2);
      }
      final int rows = Integer.parseInt(otherArgs[0]);
      final int cols=  Integer.parseInt(otherArgs[1]);
      final String inputDir = otherArgs[2];
      final String  output = otherArgs[3];
      logger.info("Input source  = " + inputDir);
      logger.info("Output destination = " + output);

      Configuration jobConf = new JobConf(conf, getClass());
      jobConf.addResource("/hadoop-distro/conf/core-site.xml");
      jobConf.addResource("/hadoop-distro/conf/mapred-site.xml");
      jobConf.addResource("/hadoop-distro/conf/hdfs-site.xml");
      multiply( inputDir,  output, jobConf);
      return 0;
}
}

