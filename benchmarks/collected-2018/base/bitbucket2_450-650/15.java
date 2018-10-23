// https://searchcode.com/api/result/102256862/

package com.virkaz.star.hadoopio;

import java.io.IOException;
import java.util.StringTokenizer;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.StringTokenizer;
import java.util.List;
import java.util.ArrayList;
import org.apache.hadoop.filecache.DistributedCache;

import org.apache.commons.io.EndianUtils;
import org.apache.hadoop.conf.Configured;
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
import org.apache.hadoop.mapred.lib.ChainMapper;
import org.apache.hadoop.mapred.lib.ChainReducer;
import org.apache.hadoop.mapreduce.Mapper.Context;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.FileSplit;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.SequenceFileOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.apache.hadoop.util.GenericOptionsParser;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.mapred.JobConf;
//import org.apache.hadoop.mapred.JobConf;
/**
 * Input to the mapper is the data file to process. The data file name is then handed off to 
 * a C++ analysis function which then reads in elements of the file, performs an analysis and then 
 * writes the file to some root data structure and returns the filename.
 * The reduce step then just writes the local file to HDFS
 */
@SuppressWarnings("deprecation")
public class ProcessAndCopy extends Configured implements Tool {
    // This should provide the ability to perform operations on a root file: 1) write values locall to a TTree
    /*
    static {
	System.loadLibrary("processFile");
    }
    */
    private static final Log logger = LogFactory.getLog(ProcessAndCopy.class);
    
    protected static final String destDir = "/user/hduser/store";
    protected static final String localDir = "/hadoop-distro/data";
    private static final String libUri= 
			"hdfs://azad:54310/libraries/libprocessFile.so#libprocessFile.so";
    public static final boolean testingMode = true;

    // This will pass the local file path to the C++ function, which returns the root file
    protected static native String  processFile(String path);

    public static String processFileWithRoot(String path){
	return processFile(path);
    }
    // Take a vector, perform some transformation on it, store the result in TTree, write everything out to root file
    //protected static native String  transformVectorToRootFile(String path, double data[]);

    //TextOutputFormat to;
    /**
     * Ideally this would be a chain of mappers.
     * Will collapse it and leave the chain for later.
     * @author charlescearl
     *
     */
    public static class RawDataToRootMapper
       extends Mapper<NullWritable, BytesWritable,Text, Text>{
    
		public void map(NullWritable key, BytesWritable data, Context context
        ) throws IOException, InterruptedException {
		    // This is better to do in the reduce step where we have access to the destination path
		    loadPCLibrariesMR();
		    FileSplit fs = (FileSplit)context.getInputSplit();
		    logger.info("Running the job");
		    FileSystem filesys= fs.getPath().getFileSystem(context.getConfiguration());
		    Path dfsPath= fs.getPath();
		    String finalFileName= dfsPath.getName();
		    Path tmpPath= new Path(ProcessAndCopy.localDir,finalFileName);
		    logger.info("The local tmp file will be "+tmpPath);

		    /*
		    filesys.copyToLocalFile(dfsPath,tmpPath);
		    logger.info("Completed copy to local");
		    // So want to create a local copy
		    String fileName= fs.getPath().toString().split(":")[1];
		    // Using startLocalOutput, then completeLocalOutput when done 
		    logger.info("Passing file "+tmpPath.toString()+" to processFile");
		    String resultFile= processFile(tmpPath.toString());
		    if(resultFile != null){
			logger.info("File "+resultFile+" was created locally");
		    	Path srcPath= new Path(resultFile);
		    	Path destPath= new Path(destDir,srcPath.getName());
			logger.info("File "+destPath.toString()+" is the desired destination");
		    	filesys.copyFromLocalFile(srcPath,
		    			destPath);
		    	context.write(new Text(fileName), new Text(destPath.toString()));
			}*/
		}}

    /**
     * Use this for the ChainMapper
     * @author charlescearl
     *
     */
    public static class CopyLocalRootToHDFSMapper
       extends Mapper<NullWritable, Text,Text, Text>{
    
	private static final Log logger = LogFactory.getLog(ProcessAndCopy.class);
	
	public void map(NullWritable key, Text localFileName, Context context
        ) throws IOException, InterruptedException {
		    // This is better to do in the reduce step where we have access to the destination path
		    //loadPCLibrariesMR();

		    if(localFileName.getLength() != 0){
			// Need the filesys in order to do the copy
			FileSystem filesys= FileSystem.get(context.getConfiguration());
			// Get the name of the file in which the local root is stored
			Path resultPath = new Path(localFileName.toString());
			// Get the HDFS destination
			Path destPath= new Path(destDir,resultPath.getName());
			// Do the copy
			filesys.copyFromLocalFile(resultPath,destPath);
			//context.write(new Text(fs.toString()), new DoubleWritable(result));
			context.write(localFileName, new Text(destPath.toString()));}
			//output.collect(key, bmr);
	}}
  
  
  public static class RawToRootReducer 
       extends Reducer<Text,Text,Text,Text> {
    //private DoubleWritable result = new DoubleWritable();

      public void reduce(Text key, Iterable<Text> values, 
			 Context context
			 ) throws IOException, InterruptedException {
	  //double sum = 0.0;
	  // Now iterate over the local files and copy to 
	  // Just compose a list of the root files that were written
	  //	  ProcessAndCopy.loadPCLibrariesMR();
	  Text result = new Text();

	  StringBuffer sbuf= new StringBuffer("Files written: ");
	  for(Text destFilename : values){
	      sbuf.append(" ").append(destFilename.toString());
	  }
	  //EndianUtils.writeSwappedDouble(dataArray, 0, sum);
	  result = new Text(sbuf.toString());
	  //result.set(sum);
	  context.write(key, result);
      }
  }
    /*
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
    */


 public void fileProcessOperations(String input, String output, Configuration conf){
	 
	    Job job;
	    logger.info("Loading resources");
	    try {
		//logger.info("Loading resources");
		//MatrixUtils.loadMatrixUtils(conf);
		//		
		loadPCLibraries(conf);
		JobConf jobConf = new JobConf(conf, ProcessAndCopy.class);
		job = new Job(jobConf,"raw_to_root");
	         
	         // Specify various job-specific parameters     
		//job = new Job(conf, "raw to root processor");
		//JobConf jobConf = new JobConf(conf, getClass());
		// job.setJobName("raw_to_root_processor");
		/*
		JobConf mapRawToRoot = new JobConf(false);
		
		// Mapper<NullWritable, BytesWritable,NullWritable, Text>
		
		ChainMapper.addMapper(jobConf, RawDataToRootMapper.class,
				NullWritable.class, BytesWritable.class,
				NullWritable.class, Text.class, true, mapRawToRoot);
		ChainMapper.addMapper((JobConf)jobConf, RawDataToRootMapper.class, 
				NullWritable.class, BytesWritable.class,
				NullWritable.class, Text.class, true, mapRawToRoot);
		
		JobConf mapWriteRootToHDFS = new JobConf(false);

		ChainMapper.addMapper(jobConf, CopyLocalRootToHDFSMapper.class, NullWritable.class, 
				Text.class,
				      Text.class,Text.class, false, mapWriteRootToHDFS);
 


		JobConf reduceConf = new JobConf(false);

		ChainReducer.setReducer(conf, RawToRootReducer.class, Text.class, Text.class,
					Text.class, Text.class, true, reduceConf);
 
		*/

		// Not so sure about this part
		// Will make the minimal initial assumption that I just have to set the input and output formats

		job.setJarByClass(ProcessAndCopy.class);
		job.setMapperClass(RawDataToRootMapper.class);
		job.setInputFormatClass(BinaryMatrixInputFormat.class);
		job.setOutputFormatClass(TextOutputFormat.class);
		//job.setOutputFormatClass(FileOutputFormat.class);
		//job.setCombinerClass(MatrixSumReducer.class);
		job.setReducerClass(RawToRootReducer.class);
		job.setMapOutputKeyClass(NullWritable.class);
		job.setMapOutputValueClass(Text.class);
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(Text.class);
		//job.setOutputValueClass(BytesWritable.class);
		//BinaryMatrixInputFormat.addInputPath(job, new Path(otherArgs[0]));
		// I think this will 
		logger.info("Setting input and output paths");
		    //Path inputPath = new Path(input);
		//BinaryMatrixInputFormat.addInputPath(job, new Path(input));
		    //FileOutputFormat.setOutputPath(job, new Path(otherArgs[1]));
		//SequenceFileOutputFormat.setOutputPath(job, new Path(output));
		logger.info("Finished configuration");
		logger.info("Configuration is "+job.getConfiguration().toString());
		    //job.submit();
		    //JobClient.runJob((JobConf)job.getConfiguration());
		    //JobClient.runJob(arg0)

		// JobClient jc = new JobClient(conf);
		// RunningJob job = jc.submitJob(conf);
		FileInputFormat.setInputPaths(job, new Path(input));
		FileOutputFormat.setOutputPath(job, new Path(output));

		
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
		
		*/  
		catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	    catch (ClassNotFoundException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	    } catch (InterruptedException e) {
			// TODO Auto-generated catch block
		e.printStackTrace();
	    } 
 }
    /*
    public static void main(String[] args) {
	String nextFileName= processFile("testfile.root");
	System.out.println(nextFileName);
    }
    */
    // What is the best way to configure when we need to pass in directories for input and output
    @SuppressWarnings("deprecation")
	public static void main(String[] args) throws Exception {
	  
	//Configuration conf = getConf();
	int ret = ToolRunner.run(new ProcessAndCopy(), args);
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


	public  void loadPCLibraries(Configuration conf) throws URISyntaxException{

		DistributedCache.createSymlink(conf); 
		DistributedCache.addCacheFile(new URI(libUri), conf);
		try{
		    
		    for(URI uri : DistributedCache.getCacheFiles(conf))
			System.out.println("Cache files are "+
					   uri);		    
		}
	    catch(IOException e){
		System.out.println("Could not read in loadPCLibraries");
		
		//DistributedCache.addCacheFile(libUri, conf);
		//System.loadLibrary("libmatrixUtilsImp.so"); 
		//System.loadLibrary("libmatrixUtilsImp.so");

		//System.load("/hadoop-distro/lib/libmatrixUtilsImp.so")
	    }
	}

	public static synchronized void loadPCLibrariesMR() {
	    //DistributedCache.createSymlink(conf); 
	    //	DistributedCache.addCacheFile(new URI(libUri), conf);
	    //System.loadLibrary("libmatrixUtilsImp.so"); 
	    try{
		//		System.loadLibrary("libprocessFile.so"); 
		System.load("/lib/x86_64-linux-gnu/libc.so.6");
		System.out.println("Read libc");
		System.load("/lib/x86_64-linux-gnu/libgcc_s.so.1");
		System.out.println("Read libcc");
		System.load("/lib/x86_64-linux-gnu/libm.so.6");
		System.out.println("Read libm");
		System.load("/usr/lib/x86_64-linux-gnu/libstdc++.so.6");
		System.out.println("Read stdc");
		System.load("/lib/x86_64-linux-gnu/libdl.so.2");
		System.out.println("Loading lib dl");
		System.load("/lib/x86_64-linux-gnu/libcrypt.so.1");
		System.out.println("Loading lib crypt");
		System.load("/lib/x86_64-linux-gnu/libpcre.so.3");
		System.out.println("Loading lib pcre");
		System.load("/usr/local/lib/libz.so.1.2.6");
		System.out.println("Loading libz");
		System.out.println("Trying root libraries");
		// System.load("/lib/x86_64-linux-gnu/libpthread.so.0");
		System.load("/hadoop-distro/lib/libCint.so");
		// System.load("/media/LinuxShare2/root/lib/libCint.so");
		System.out.println("Read Cint");
		System.load("/hadoop-distro/lib/libCore.so");
		//     System.load("/media/LinuxShare2/root/lib/libCore.so");
		System.out.println("Read libCore");

		System.load("/hadoop-distro/lib/libThread.so");
		System.out.println("Read Thread");
		System.load("/hadoop-distro/lib/libRIO.so");
		System.out.println("Read RIO");
		System.load("/hadoop-distro/lib/libMathCore.so");
		System.out.println("Read MathCore");
		System.load("/hadoop-distro/lib/libNet.so");
		System.out.println("Read Net");

		System.load("/hadoop-distro/lib/libTree.so");
		System.out.println("Read libTree");

		/*

	
	linux-vdso.so.1 =>  (0x00007fff779d2000)
	libTree.so => /media/LinuxShare2/root-5.32.00/lib/libTree.so (0x00007f02bdfd4000)
	libCore.so => /media/LinuxShare2/root-5.32.00/lib/libCore.so (0x00007f02bd62b000)
	libCint.so => /media/LinuxShare2/root-5.32.00/lib/libCint.so (0x00007f02bccab000)
	libRIO.so => /media/LinuxShare2/root-5.32.00/lib/libRIO.so (0x00007f02bc89b000)
	libThread.so => /media/LinuxShare2/root-5.32.00/lib/libThread.so (0x00007f02bc647000)
	libNet.so => /media/LinuxShare2/root-5.32.00/lib/libNet.so (0x00007f02bc2c5000)
	libMathCore.so => /media/LinuxShare2/root-5.32.00/lib/libMathCore.so (0x00007f02bbe7d000)
	libstdc++.so.6 => /usr/lib/x86_64-linux-gnu/libstdc++.so.6 (0x00007f02bbb77000)
	libm.so.6 => /lib/x86_64-linux-gnu/libm.so.6 (0x00007f02bb8f1000)
	libgomp.so.1 => /usr/lib/x86_64-linux-gnu/libgomp.so.1 (0x00007f02bb6e3000)
	libgcc_s.so.1 => /lib/x86_64-linux-gnu/libgcc_s.so.1 (0x00007f02bb4cd000)
	libpthread.so.0 => /lib/x86_64-linux-gnu/libpthread.so.0 (0x00007f02bb2ae000)
	libc.so.6 => /lib/x86_64-linux-gnu/libc.so.6 (0x00007f02baf1a000)
	libdl.so.2 => /lib/x86_64-linux-gnu/libdl.so.2 (0x00007f02bad16000)
	libz.so.1 => /lib/x86_64-linux-gnu/libz.so.1 (0x00007f02baafd000)
	libpcre.so.3 => /lib/x86_64-linux-gnu/libpcre.so.3 (0x00007f02ba8c1000)
	libcrypt.so.1 => /lib/x86_64-linux-gnu/libcrypt.so.1 (0x00007f02ba688000)
	libcrypto.so.0.9.8 => /lib/libcrypto.so.0.9.8 (0x00007f02ba2f8000)
	libssl.so.0.9.8 => /lib/libssl.so.0.9.8 (0x00007f02ba0a5000)
	/lib64/ld-linux-x86-64.so.2 (0x00007f02be65b000)
	librt.so.1 => /lib/x86_64-linux-gnu/librt.so.1 (0x00007f02b9e9c000)
		*/
	    }
	    catch(UnsatisfiedLinkError e){
		System.out.println("Got link error "+e);
		//System.load("/hadoop-distro/lib/libprocessFile.so");
		//System.loadLibrary("libmatrixUtilsImp.so");
		//System.load("/hadoop-distro/lib/libmatrixUtilsImp.so");
	    }
	    
	}


}

