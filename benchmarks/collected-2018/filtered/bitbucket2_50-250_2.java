// https://searchcode.com/api/result/59415666/

package local.jv.rsa;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.SequenceFile;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.SequenceFileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.SequenceFileOutputFormat;


/**
 * THIS IS REFERENCE ONLY, SKELETON IS NOT WORKED THROUGH, NO PROBLEM SOLVED HERE :)
 * 
 * @author jv
 */
class Map extends Mapper<LongWritable, LongWritable, LongWritable, LongWritable> {
	private long n = 0;

	/** Reducer setup */
	public void setup (Context context) {
		//Get the value of n from the job configuration
		n = context.getConfiguration().getLong("n", 0);
	}

    @Override
    protected void map(LongWritable key, LongWritable value, Context context) throws IOException, InterruptedException {
    	LongWritable new_key = new LongWritable(0);
    	LongWritable new_value =  new LongWritable(0);
    	
    	//TODO: Define the MAP function
    	
		//key   - n
		//value - into how many ranges the work should be divided
	
		//define content of map
    
    	context.write(new_key, new_value);
    }
}

class Reduce extends Reducer<LongWritable, LongWritable, LongWritable, LongWritable> {
	private long n = 0;
	
	/** Reducer setup */
	public void setup (Context context) {
		//Get the value of n from the job configuration
		n = context.getConfiguration().getLong("n", 0);
	}

	
    @Override
    protected void reduce(LongWritable key, Iterable<LongWritable> values, Context context) throws IOException, InterruptedException {
    	LongWritable new_key = new LongWritable(0);
    	LongWritable new_value = new LongWritable(0);
    	
    	//TODO: Define the REDUCE function
    	
    	for(LongWritable value : values){
    		
		//define content of reduce

        	context.write(new_key, new_value);

    	}
    }
}

public class MapReduceSkeleton {
	static Configuration conf;
	static Log log = LogFactory.getLog(MapReduceSkeleton.class);
	
	public int run(String pathin, String pathout) throws Exception {
		Job job = new Job(conf);
		job.setJarByClass(MapReduceSkeleton.class);

		job.setMapperClass(Map.class);
		job.setReducerClass(Reduce.class);
        
		job.setInputFormatClass(SequenceFileInputFormat.class);
		job.setOutputFormatClass(SequenceFileOutputFormat.class);
		
		job.setMapOutputKeyClass(LongWritable.class);
		job.setMapOutputValueClass(LongWritable.class);
		
		job.setOutputKeyClass(LongWritable.class);
		job.setOutputValueClass(LongWritable.class);	
	
		job.setNumReduceTasks(conf.getInt("nr_of_blocks", 1));
		
		FileInputFormat.addInputPath(job, new Path(pathin));
		FileOutputFormat.setOutputPath(job, new Path(pathout));

		boolean success = job.waitForCompletion(true);
		return success ? 0 : -1;
    }
	

    public static void main(String[] args) throws Exception {
    	MapReduceSkeleton skel = new MapReduceSkeleton();
    	
    	conf = new Configuration();
    	
    	if(args.length < 4) {
    		System.out.println("Too few arguments. Arguments should be: " +
    				"<value of n - the number to factorize> " +
    				"<hdfs input folder> " +
    				"<hdfs output folder> " +
    				"<num of blocks to divide the computation>");
    		System.out.println("For excample: 67771 inputFolderFirstname outputFolderFirstname 5");
    		System.exit(0);
    	}
    	
    	long n = Long.parseLong(args[0]);
 
     	
    	String pathin = args[1];
    	String pathout = args[2];
    	int ranges = Integer.parseInt(args[3]);
    	
    	conf.setLong("n", n);
    	conf.setLong("nr_of_blocks", ranges);
    	
    	writeNumber(n, ranges, pathin);
        
    	skel.run(pathin, pathout);
        
        ArrayList<Long> factors = readNumber(pathout); 
        
        log.info("Result: ");
        for (long factor : factors){
        	log.info(factor);
        }
    }
    
    
    // Method to use this factorization from another Java class directly
    public static ArrayList<Long>  factor(long n, long ranges, String  pathin, String pathout) throws Exception {
    	
    	MapReduceSkeleton skel = new MapReduceSkeleton();
    	
    	conf = new Configuration();
        	
    	conf.setLong("n", n);
    	conf.setLong("nr_of_blocks", ranges);
    	
    	writeNumber(n, ranges, pathin);
        
        skel.run(pathin, pathout);
        
        ArrayList<Long> factors = readNumber(pathout); 
        
        return factors;
    }
    
    
	/**
	 * Method to write a number to a file on HDFS, acts as an input for MapReduce
	 */
    
    public static void writeNumber (long number, long ranges,  String pathStr)
	throws IOException
	{
		FileSystem fs = FileSystem.get(conf);
		
		Path path = new Path(pathStr);
		SequenceFile.Writer writer = SequenceFile.createWriter(fs, conf, path, 
			LongWritable.class, LongWritable.class, 
			SequenceFile.CompressionType.NONE);
		
		LongWritable index = new LongWritable(number);
		LongWritable num = new LongWritable(ranges);
		writer.append(index, num);
		writer.close();
	}
    
    
	/**
	 * Method to read numbers (factors) from files on a folder in HDFS
	 */
	public static ArrayList<Long> readNumber (String pathStr) throws IOException
	{
		ArrayList<Long> factors = new ArrayList<Long>();
		
		FileSystem fs = FileSystem.get(conf);
		Path path = new Path(pathStr);
		
		if(!fs.isFile(path)){
			FileStatus[] listFiles = fs.listStatus(path);
			for(int i = 0; i < listFiles.length; i++){
				try{
					SequenceFile.Reader reader = new SequenceFile.Reader(fs, listFiles[i].getPath(), conf);
					LongWritable index = new LongWritable();
					LongWritable el = new LongWritable();
					while (reader.next(index, el)) {
						factors.add(el.get());
					}
					reader.close();
				}
				catch(Exception e){

				}
			}
		}
		return factors;
	}
    
}

