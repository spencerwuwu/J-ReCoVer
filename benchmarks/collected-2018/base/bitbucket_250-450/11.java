// https://searchcode.com/api/result/102256868/

package com.virkaz.star.hadoopio;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.StringTokenizer;

import org.apache.commons.io.EndianUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.filecache.DistributedCache;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.DistributedFileSystem;
import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.IOUtils;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.JobClient;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.OutputCollector;
import org.apache.hadoop.mapred.Reporter;
import org.apache.hadoop.mapred.TextOutputFormat;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.Mapper.Context;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.FileSplit;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.SequenceFileOutputFormat;
import org.apache.hadoop.util.GenericOptionsParser;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

public class FileProcessOperations extends Configured implements Tool {
	
	private static final Log logger = LogFactory.getLog(FileProcessOperations.class);
	public static final boolean testingMode = true;


  public static class FileProcessMapper 
       extends Mapper<NullWritable, BytesWritable,Text, Text>{
		/*
		public void map(NullWritable key, BytesWritable data,
				OutputCollector<NullWritable, BytesWritable> output, Reporter reporter)
		throws IOException {
			*/
		public void map(NullWritable key, FileInputFormat data, Context context
        ) throws IOException, InterruptedException {
			// There is no split of the file, just get file name,
			// it will be processed, and the path will be sent
			FileSplit fs = (FileSplit)context.getInputSplit();
			System.out.println("Running the job");
			String fileName= fs.getPath().toString().split(":")[1];
			logger.info("The file being processed is "+fileName);
			String localTmpFile= processFileLocally(fileName);
			// Everything happens here.
			context.write(new Text(fileName), new Text(localTmpFile));
			//output.collect(key, bmr);
			
			
		}
		
		/**
		 * Read the original file, compute some function of the input,
		 * Store the values in a root file.
		 * @param originalFile
		 * @return
		 */
		private String processFileLocally(String originalFile){
			String newFile= originalFile+".root";
			return newFile;
		}
  }
  
  public static class MatrixSumReducer 
       extends Reducer<Text,Text,Text,Text> {
    private BytesWritable result = new BytesWritable();

    public void reduce(Text key, Iterable<Text> values, 
                       Context context
                       ) throws IOException, InterruptedException {
    	StringBuffer sb= new StringBuffer();
    	Configuration conf= context.getConfiguration();
    	String destFile= null;
    	Text fileComparison= new Text();
    	String localFile=key.toString();
    	int checkCount= 0;
    	String hash1= null,hash2= null;
		FileSystem fs=new DistributedFileSystem();
		try {
			fs.initialize(new URI("namenode_uri"), conf);
	    	//double sum = 0.0;
		      // There should be one value per key. 
		      // Key is the original filename and value will be the name of the hdfs file
		      //byte[] dataArray = new byte[8];
		    	// Reduce just verifies that: 1. There is one value,
		    	// 2. SHA-256 hash of both files matches
		    	// just write <filename:{hash,hash2,ok}> if they match
		    	//            <filename:{hash1,hash2,bad> or {hash1,_,bad} if we did not get that far
		      for (Text val : values) {
		    	  destFile= val.toString();
		    	  checkCount++;
		        //val.get();
		      }
		      hash1 = determineFileHash(localFile,fs);
		      if(checkCount > 1){
		    	  sb.insert(0, localFile)
		    	  .append(",")
		    	  .append(hash1)
		    	  .append(",_")
		    	  .append(",bad");
		    	  
		      }
		      else{
		    	  hash2= determineFileHash(destFile,fs);
		    	  sb.insert(0, localFile)
		    	  .append(",")
		    	  .append(hash1)
		    	  .append(",_")
		    	  .append(",")
		    	  .append(hash2);
		    	  
		      }
		      context.write(key,new Text(sb.toString()));
		      logger.info("Hash of file1 is "+hash1);
		      logger.info("Hash of file2 is "+hash2);
		 
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
   }

    /**
     * Initially just return the filename
     * @param localFile
     * @param fs
     * @return
     */
    private String determineFileHashMock(String localFile, FileSystem fs){
    	return localFile;
    }
	private String determineFileHash(String localFile, FileSystem fs) {
		// TODO Auto-generated method stub
		
		String fileHash= null;
		if(FileProcessOperations.testingMode)
			return localFile;
		
		
		MessageDigest md;
		try {
			md = MessageDigest.getInstance("SHA-256");
			FileInputStream fi = new FileInputStream (localFile);
		    
			// Need to replace this with FSInputFileStream
			//FSDataInputStream fis = new FSDataInputStream(localFile);
	 
	        byte[] dataBytes = new byte[1024];
	        boolean flag=true;
	        while(flag){
	        try{
	        	// Needs to be finer grained in terms of bytes read.
	        	IOUtils.readFully(fs.open(new Path(localFile)), dataBytes, 0,1024);
	        	md.update(dataBytes, 0, 1024);
	        }
	        catch(IOException e){
	        	flag = false;
	        }}
	 
	        byte[] mdbytes = md.digest();
	 
	        //convert the byte to hex format method 1
	        StringBuffer sb = new StringBuffer();
	        for (int i = 0; i < mdbytes.length; i++) {
	          sb.append(Integer.toString((mdbytes[i] & 0xff) + 0x100, 16).substring(1));
	        }
	 
	        System.out.println("Hex format : " + sb.toString());
	 
	       //convert the byte to hex format method 2
	        StringBuffer hexString = new StringBuffer();
	    	for (int i=0;i<mdbytes.length;i++) {
	    	  hexString.append(Integer.toHexString(0xFF & mdbytes[i]));
	    	}
	 
	    	System.out.println("Hex format : " + hexString.toString());
			return fileHash;

		} catch (NoSuchAlgorithmException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return fileHash;
	}
  }

  
 public void fileProcessOperations(String input, String output, Configuration conf){
	 
	    Job job;
		try {
			//logger.info("Loading resources");
			//MatrixUtils.loadMatrixUtils(conf);
			job = new Job(conf, "data file processor");
		    job.setJarByClass(FileProcessOperations.class);
		    job.setMapperClass(FileProcessMapper.class);
		    job.setInputFormatClass(FileInputFormat.class);
		    //job.setOutputFormatClass(cls)
		    job.setOutputFormatClass(FileOutputFormat.class);
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
		*/  catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
 }
 
 // What is the best way to configure when we need to pass in directories for input and output
  @SuppressWarnings("deprecation")
public static void main(String[] args) throws Exception {
	  
    //Configuration conf = getConf();
    int ret = ToolRunner.run(new FileProcessOperations(), args);
    System.exit(ret);
  }

@Override
public int run(String[] args) throws Exception {
	// TODO Auto-generated method stub
          
      Configuration conf= getConf();
      String[] otherArgs = new GenericOptionsParser(conf, args).getRemainingArgs();
      if (otherArgs.length != 2) {
        System.err.println("Usage: fileProcess <in> <out>");
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
      fileProcessOperations( inputDir,  output, jobConf);

	return 0;
}
}

