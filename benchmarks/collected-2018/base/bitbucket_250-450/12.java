// https://searchcode.com/api/result/102256870/

package com.virkaz.star.hadoopio;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.StringTokenizer;
import java.util.List;
import java.util.ArrayList;

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
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.Mapper.Context;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.SequenceFileOutputFormat;
import org.apache.hadoop.util.GenericOptionsParser;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

public class MatrixOperations extends Configured implements Tool {
	
    private static final Log logger = LogFactory.getLog(MatrixOperations.class);
    private static int INPUT_VECTOR_SIZE= 5;
    private static int OUTPUT_VECTOR_SIZE= 5;


  public static class MatrixSumMapper 
       extends Mapper<NullWritable, BytesWritable, NullWritable, BytesWritable>{
    
		//private static final Log logger = LogFactory.getLog(BinaryMatrixMapper.class);
		private static final int doubleOffset = 8;

		/*
		public void map(NullWritable key, BytesWritable data,
				OutputCollector<NullWritable, BytesWritable> output, Reporter reporter)
		throws IOException {
			*/
		public void map(NullWritable key, BytesWritable data, Context context
        ) throws IOException, InterruptedException {
			// Process the 
		    MatrixUtils.loadMatrixUtilsMR();
		    // Create a random matrix for the transformation
		    // double [][] dataMatrix= MatrixUtils.mCreate(INPUT_VECTOR_SIZE,OUTPUT_VECTOR_SIZE);
		    //		    double [][] dataMatrix= {{5.,7,1.0},{.5,.7,1.0},{.5,.7,1.0},{.5,.7,1.0},{.5,.7,1.0}};
		    double [][] dataMatrix= {{1.,0.,0.,0.,0.},{.0,1.,0.,0.,0.},{0.,0.,1.,0.,0.},{0.,0.,0.,1.,0.},
					     {0.,0.,0.,0.,1.}};
		    // Read a row
		    double [] returnVector;
		    // Going to transform to smaller vector
		    double [] dataVector= new double[INPUT_VECTOR_SIZE];
		    double [] writeVector= new double[OUTPUT_VECTOR_SIZE];
		    byte[] bArray= data.getBytes();
		    ByteArrayOutputStream bos= new ByteArrayOutputStream();
		    PrintStream ps= new PrintStream(bos);
		    double dValue;
		    StringBuffer sb;
		    System.out.println("Starting the job");
		    logger.info("Running the job");
		    logger.info("Byte length of the BytesWritable data "+bArray.length);
		    BytesWritable bmr= new BytesWritable();
		    byte[] dataArray = new byte[OUTPUT_VECTOR_SIZE*8];

		    // double result= 0;
		    // result= com.virkaz.star.hadoopio.MatrixUtils.mRank(dataMatrix);
		    for(int i = 0;i<INPUT_VECTOR_SIZE;i++){
			// run test here
			//			for (int j= 0;j<INPUT_VECTOR_SIZE;j++){
			sb= new StringBuffer();	
			//			dValue= EndianUtils.readSwappedDouble(bArray,(i*INPUT_VECTOR_SIZE +j)*doubleOffset);
			dValue= EndianUtils.readSwappedDouble(bArray,i*doubleOffset);
			sb.append("Value at ").append(i).append(" is ").append(dValue);
			logger.info(sb.toString());
			dataVector[i]= dValue;
			//			    ps.format("Value of element %d inside map read is %.4f%n",i,dValue);
				//result += dValue;
			//  logger.info(bos.toString());
			    // Assert.assertTrue(Math.abs(dValue - correctValues[i]) < 0.00001);
		    }
		    // This is not getting the correct result
		    writeVector= MatrixUtils.mVectorMultiply(dataVector,dataMatrix);
		    for (int i =0;i< OUTPUT_VECTOR_SIZE;i++){
			sb= new StringBuffer();	
			sb.append("Value at ").append(i).append(" is ").append(writeVector[i]);
			logger.info(sb.toString());			
			EndianUtils.writeSwappedDouble(dataArray, i*8, writeVector[i]);
		    }
			
		    //		    }
			
			//result= com.virkaz.star.hadoopio.MatrixUtils.mRank(dataMatrix);
	        //      System.out.println("First value is "+inputVal);

		    bmr.set(dataArray, 0, OUTPUT_VECTOR_SIZE*8);
		    //bmr.set(result);
		    //logger.info("Result of map is "+result);
		    //System.out.println("Sending output..."+result);
		    context.write(key, bmr);
			//context.
			//output.collect(key, bmr);
		}

  }
  
    
  public static class MatrixSumReducer 
       extends Reducer<NullWritable,BytesWritable,NullWritable,BytesWritable> {
    private BytesWritable result = new BytesWritable();

    public void reduce(NullWritable key, Iterable<BytesWritable> values, 
                       Context context
                       ) throws IOException, InterruptedException {
	MatrixUtils.loadMatrixUtilsMR();

      double sum = 0.0;
      List<Double []> resultList= new ArrayList<Double []>();
      Double [] storageArray;
      int row= 0;
      int col;
      for (BytesWritable val : values) {
	  storageArray = new Double[OUTPUT_VECTOR_SIZE];
    	  for(col=0; col< OUTPUT_VECTOR_SIZE;col++)
	      // EndianUtils.writeSwappedDouble(dataArray, 0, sum);
	      storageArray[col] = EndianUtils.readSwappedDouble(val.getBytes(), col*8);
	  resultList.add(storageArray);
        //val.get();
      }
      //      logger.info("Result of reduce is "+sum);
      // is this too big to be in memory??
      byte[] dataArray = new byte[8*OUTPUT_VECTOR_SIZE*resultList.size()];
      // Just need to concatenate the output
      for(Double [] aVec : resultList)
	  {
	      // This needs to be an EndianUtils call that writes the data out...
	      for(col = 0;col < OUTPUT_VECTOR_SIZE;col++)
		  // result.set(dataArray, (row*OUTPUT_VECTOR_SIZE+col), 8);
		  {
		      EndianUtils.writeSwappedDouble(dataArray,8*(row*OUTPUT_VECTOR_SIZE+col),aVec[col]);
		      logger.info("Writing entry "+row+","+col+" which is "+aVec[col]);
		  }
	      row++;
	  }
      result.set(dataArray,0,8*OUTPUT_VECTOR_SIZE*resultList.size());
      //result.set(sum);
      context.write(key, result);
    }
  }

  
 public void multiply(String input, String output, Configuration conf){
     Job job;
     try {
	 logger.info("Loading resources");
	 MatrixUtils.loadMatrixUtils(conf);
	 job = new Job(conf, "matrix multiply");
	 job.setJarByClass(MatrixOperations.class);
	 job.setMapperClass(MatrixSumMapper.class);
	 job.setInputFormatClass(BinaryMatrixInputFormat.class);
	 //job.setOutputFormatClass(cls)
	 job.setOutputFormatClass(SequenceFileOutputFormat.class);
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
	 BinaryMatrixInputFormat.addInputPath(job, new Path(input));
	 //FileOutputFormat.setOutputPath(job, new Path(otherArgs[1]));
	 SequenceFileOutputFormat.setOutputPath(job, new Path(output));
	 logger.info("Finished configuration");
	 logger.info("Configuration is "+job.getConfiguration().toString());
	 //job.submit();
	 //JobClient.runJob((JobConf)job.getConfiguration());
	 //JobClient.runJob(arg0)
	 System.exit(job.waitForCompletion(true) ? 0 : 1);	 
	 
     } catch (IOException e) {
	 // TODO Auto-generated catch block
	 e.printStackTrace();
     } 
		/*
		catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		
		catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		*/  
     catch (ClassNotFoundException e) {
	 // TODO Auto-generated catch block
	 e.printStackTrace();
     } catch (InterruptedException e) {
	 // TODO Auto-generated catch block
	 e.printStackTrace();
     } catch (URISyntaxException e) {
	 // TODO Auto-generated catch block
	 e.printStackTrace();
     }
 }
 
 // What is the best way to configure when we need to pass in directories for input and output
  @SuppressWarnings("deprecation")
public static void main(String[] args) throws Exception {
	  
    //Configuration conf = getConf();
    int ret = ToolRunner.run(new MatrixOperations(), args);
    System.exit(ret);
  }

@Override
public int run(String[] args) throws Exception {
	// TODO Auto-generated method stub
          
      Configuration conf= getConf();
      String[] otherArgs = new GenericOptionsParser(conf, args).getRemainingArgs();
      if (otherArgs.length != 2) {
        System.err.println("Usage: matrixOps <in> <out>");
        System.exit(2);
      }
      final String inputDir = otherArgs[0];
      final String  output = otherArgs[1];
      logger.info("Input source  = " + inputDir);
      logger.info("Output destination = " + output);

     Configuration jobConf = new JobConf(conf, getClass());
      jobConf.addResource("/hadoop-distro/conf/core-site.xml");
      jobConf.addResource("/hadoop-distro/conf/mapred-site.xml");
      jobConf.addResource("/hadoop-distro/conf/hdfs-site.xml");
      
      //System.out.println("Estimated value of Pi is " + estimate(nMaps, nSamples, jobConf));
      multiply( inputDir,  output, jobConf);

	return 0;
}
}

