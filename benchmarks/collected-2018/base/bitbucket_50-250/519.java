// https://searchcode.com/api/result/121822010/

package contrail;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.apache.avro.mapred.AvroJob; 
import org.apache.avro.mapred.AvroWrapper;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.FileInputFormat;
import org.apache.hadoop.mapred.FileOutputFormat;
import org.apache.hadoop.mapred.JobClient;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.MapReduceBase;
import org.apache.hadoop.mapred.Mapper;
import org.apache.hadoop.mapred.OutputCollector;
import org.apache.hadoop.mapred.Reporter;
import org.apache.hadoop.mapred.RunningJob;
import org.apache.hadoop.mapred.TextInputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.apache.log4j.Logger;
import org.apache.hadoop.mapred.lib.NLineInputFormat;


/**
 * Map reduce job to encode FastQ files in sequence files using AVRO.
 * DNA sequences are encoded as byte arrays using  7 bit ASCII (which is compatible
 * with UTF-8 the encoding used by org.apache.hadoop.io.Text).
 * 
 * We encode the data as byte arrays and try to avoid convertng it to a String because
 * toString() is expensive.
 *
 */
public class FastqPreprocessorAvroByte extends Configured implements Tool 
{	
	private static final Logger sLogger = Logger.getLogger(FastqPreprocessorAvroByte.class);
	
	
	/**
	 * Mapper.
	 * 
	 * @author jlewi
	 *
	 */
	private static class FastqPreprocessorMapper extends MapReduceBase 
    implements Mapper<LongWritable, Text, AvroWrapper<SequenceReadByte>, NullWritable> 
	{
		private int idx = 0;
		
		private String name = null;
		
		private String filename = null;
		
		private int mate_id = 0x0;
		private String counter = "pair_unknown";

       
        // initial size for the buffer used to encode the dna sequence
        private int START_CAPACITY = 200;
        
        private SequenceReadByte read = new SequenceReadByte();
        private AvroWrapper<SequenceReadByte> out_wrapper = new AvroWrapper<SequenceReadByte>(read);
        
        private ByteReplaceAll replacer = null; 
        
        // The byte value to replace multi-byte characters with
        // this is an underscore.
        public final byte MULTIBYTE_REPLACE_VALUE = 0x5f; 
		public void configure(JobConf job) 
		{
			filename = job.get("map.input.file");
			
			boolean usesuffix = Integer.parseInt(job.get("PREPROCESS_SUFFIX")) == 1;
			
			String suffix = null;
			if (usesuffix)
			{
				if  (filename.contains("_1.")) { 
					suffix = "_1";
					mate_id = 0x1; 
					counter = "pair_1"; 
				}
				else if (filename.contains("_2.")) { 
					suffix = "_2";
					mate_id = 0x2;
					counter = "pair_2";
				}
				else { 
					counter = "pair_unpaired"; 
				}
								
				System.err.println(filename + " suffix: \"" + suffix + "" + "\"");
			}            
      read.dna = ByteBuffer.allocate(START_CAPACITY);
      read.mate_pair_id = mate_id;
      replacer = new ByteReplaceAll(":#-.|/","_");
		}
		
		public void map(LongWritable lineid, Text line,
                OutputCollector<AvroWrapper<SequenceReadByte>, NullWritable> output, Reporter reporter)
                throws IOException 
        {
			if (idx == 0) 
			{ 
				// We operate on the bytes instead of converting to a string.
				// The advantage is that we can use our more efficient implementation
				// for replace all. 
				byte[] data = line.getBytes();
				
				// Replace any multibyte characters with "_"
				int valid_length = ByteUtil.replaceMultiByteChars(data, MULTIBYTE_REPLACE_VALUE, line.getLength());
								
				// make sure it starts with the @ symbol
				if (data[0] != 0x40)
				{					
					throw new IOException("ERROR: Invalid readname: " + line.toString() + " in " + filename);
				}
		
				// Find the location of the first space in the name.
				int end_index = valid_length-1;
				for (int index = 1; index <= end_index; index++){
					if (data[index] == 0x20){
						end_index = index -1;
						break;
					}
				}
				// Remove the leading '@' and chop everything after the first space.
				data = java.util.Arrays.copyOfRange(data, 1, end_index+1);
				
				// Replace any funny characters 				
				// with "_"
				replacer.replaceAll(data);
				
				name = new String(data, ByteReplaceAll.encoding);				
			}
			else if (idx == 1) {				 
				read.dna=ByteBuffer.wrap(line.getBytes(), 0, line.getLength());
			}
			else if (idx == 2) { }
			else if (idx == 3)
			{			
				read.id = name;				 			
				output.collect(out_wrapper, NullWritable.get());
				
				reporter.incrCounter("Contrail", "preprocessed_reads", 1);
				reporter.incrCounter("Contrail", counter, 1);
			}
			
			idx = (idx + 1) % 4;
        }
		
		public void close() throws IOException
		{
			if (idx != 0)
			{
				throw new IOException("ERROR: closing with idx = " + idx + " in " + filename);
			}
		}
	}
		
	
	// Run Tool
	///////////////////////////////////////////////////////////////////////////		
	public RunningJob run(String inputPath, String outputPath) throws Exception
	{ 
		sLogger.info("Tool name: FastqPreprocessor");
		sLogger.info(" - input: "  + inputPath);
		sLogger.info(" - output: " + outputPath);
		
		
		JobConf conf = new JobConf(Stats.class);
		conf.setJobName("FastqPreprocessorAvroByte " + inputPath);
		
		ContrailConfig.initializeConfiguration(conf);
			
		FileInputFormat.addInputPath(conf, new Path(inputPath));
		FileOutputFormat.setOutputPath(conf, new Path(outputPath));

		conf.setInputFormat(TextInputFormat.class);

		conf.setMapOutputKeyClass(Text.class);
		conf.setMapOutputValueClass(Text.class);

		conf.setMapperClass(FastqPreprocessorMapper.class);
		conf.setNumReduceTasks(0);
		
    conf.setInputFormat(NLineInputFormat.class);
    conf.setInt("mapred.line.input.format.linespermap", 2000000); // must be a multiple of 4

    // TODO(jlewi): use setoutput codec to set the compression codec. 
    AvroJob.setOutputSchema(conf,new SequenceReadByte().getSchema());
                
		//delete the output directory if it exists already
		FileSystem.get(conf).delete(new Path(outputPath), true);

    long start_time = System.currentTimeMillis();    
    RunningJob result = JobClient.runJob(conf);
    long end_time = System.currentTimeMillis();    
    double nseconds = (end_time - start_time) / 1000.0;
    System.out.println("Job took: " + nseconds + " seconds");
		return result;
	}
	

	// Parse Arguments and run
	///////////////////////////////////////////////////////////////////////////	
	public int run(String[] args) throws Exception 
	{	
		String inputPath  = args[0];
		String outputPath = args[1];
		
		ContrailConfig.PREPROCESS_SUFFIX = 1;
		ContrailConfig.TEST_MODE = true;
		
    Timer timer = new Timer("FastqPreprocessorByte");
    timer.start();
		run(inputPath, outputPath);
		timer.stop();
		System.out.println("FastqpreprocessorByte: Job took (seconds): " + timer.toSeconds());
		return 0;
	}


	// Main
	///////////////////////////////////////////////////////////////////////////	

	public static void main(String[] args) throws Exception 
	{
		int res = ToolRunner.run(new Configuration(), new FastqPreprocessorAvroByte(), args);
		System.exit(res);
	}
}

