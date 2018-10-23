// https://searchcode.com/api/result/121822013/

package contrail;

 import contrail.sequences.Alphabet;
import contrail.sequences.DNAAlphabetFactory;
import contrail.sequences.Sequence;

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
 * DNA sequences are encoded as bytes arrays. The DNA sequence is packed into an array
 * of bytes using 3 bits per letter.  
 * 
 * We encode the data as byte arrays and try to avoid convertng it to a String because
 * toString() is expensive.
 *
 */
public class FastqPreprocessorAvroCompressed extends Configured implements Tool 
{	
  private static final Logger sLogger = Logger.getLogger(FastqPreprocessorAvroCompressed.class);


  /**
   * Mapper.
   */
  public static class FastqPreprocessorMapper extends MapReduceBase 
  implements Mapper<LongWritable, Text, AvroWrapper<CompressedRead>, NullWritable> 
  {
    private int idx = 0;

    private String name = null;

    private String filename = null;

    private int mate_id = 0x0;

    /**
     *  The alphabet for encoding the sequences.
     */
    private Alphabet alphabet;

    private String counter = "pair_unknown";


    // initial size for the buffer used to encode the dna sequence
    private int START_CAPACITY = 200;

    private CompressedRead read = new CompressedRead();
    private AvroWrapper<CompressedRead> out_wrapper = new AvroWrapper<CompressedRead>(read);

    private ByteReplaceAll replacer = null; 

    // The byte value to replace multi-byte characters with
    // this an underscore.
    public final byte MULTIBYTE_REPLACE_VALUE = 0x5f;

    // The sequence.
    private Sequence sequence;
    
    // An array which can be used to tell if a UTF8 value
    // is whitespace
    private boolean[] utf8_whitespace;
    
    // Store the utf8 byte values of various characters
    private byte utf8_at;
    private byte utf8_space;
    
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
      read.mate_pair_id = mate_id;
      replacer = new ByteReplaceAll(":#-.|/$%&'()*+,-./:","_");

      alphabet = DNAAlphabetFactory.create();
      sequence = new Sequence(alphabet);
      
      // utf8_whitespace[x] = True for the utf8 charater with value
      // x, if x is a white space character. False otherwise.
      utf8_whitespace = new boolean[255];
      java.util.Arrays.fill(utf8_whitespace, false);
      
      String white_space = " \n\t";
      byte[] white_space_bytes = ByteUtil.stringToBytes(white_space);
      
      for (int pos = 0; pos < white_space_bytes.length; pos++) {
        utf8_whitespace[pos] = true;
      }
    
      utf8_at = ByteUtil.stringToBytes("@")[0];
      utf8_space = ByteUtil.stringToBytes(" ")[0];
    }

    public void map(LongWritable lineid, Text line,
        OutputCollector<AvroWrapper<CompressedRead>, NullWritable> output, Reporter reporter)
            throws IOException 
            {
      if (idx == 0) 
      { 
        // We operate on the bytes instead of converting to a string.
        // The advantage is that we can use our more efficient implementation
        // for replace all. 
        byte[] data = line.getBytes();

        // Replace any multibyte characters with "_"
        int valid_length = ByteUtil.replaceMultiByteChars(
            data, MULTIBYTE_REPLACE_VALUE, line.getLength());

        // make sure it starts with the @ symbol
        if (data[0] != utf8_at)
        {					
          throw new IOException("ERROR: Invalid readname: " + line.toString() + " in " + filename);
        }

        // Find the location of the first space in the name.
        int end_index = valid_length-1;
        for (int index = 1; index <= end_index; index++){
          if (data[index] == utf8_space){
            end_index = index -1;
            break;
          }
        }
        
        // Remove any trailing whitespace.
        while (utf8_whitespace[ByteUtil.byteToUint(data[end_index])]) {
          end_index--;
        }
        
        // Remove the leading '@' and chop everything after the first space.
        data = java.util.Arrays.copyOfRange(data, 1, end_index+1);

        // Replace any funny characters.
        replacer.replaceAll(data);

        name = new String(data, ByteReplaceAll.encoding);				
      }
      else if (idx == 1) {			  
        byte[] raw_bytes = line.getBytes();
        // TODO(jeremy@lewi.us): We should really only be checking the bytes 
        // up to line.getLength()
        if (ByteUtil.hasMultiByteChars(raw_bytes)){
          throw new RuntimeException("DNA sequence contained illegal characters. Sequence is: " + line.toString());
        }

        sequence.readUTF8(raw_bytes, line.getLength());
        int num_bytes =  (int)Math.ceil((alphabet.bitsPerLetter() * sequence.size())/ 8.0);
        
        read.setDna(ByteBuffer.wrap(sequence.toPackedBytes(), 0, num_bytes));
        read.setLength(line.getLength());
      }
      else if (idx == 2) { 
      }
      else if (idx == 3)
      {						  
        read.setId(name);				 			
        output.collect(out_wrapper, NullWritable.get());

        reporter.incrCounter("Contrail", "preprocessed_reads", 1);
        reporter.incrCounter("Contrail", counter, 1);
      }

      idx = (idx + 1) % 4;
    }

    public void close() throws IOException {
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
    sLogger.info("Tool name: FastqPreprocessorAvroCompressed");
    sLogger.info(" - input: "  + inputPath);
    sLogger.info(" - output: " + outputPath);


    JobConf conf = new JobConf(FastqPreprocessorAvroCompressed.class);
    conf.setJobName("FastqPreprocessorAvroCompressed " + inputPath);

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
    AvroJob.setOutputSchema(conf,new CompressedRead().getSchema());

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
    
    run(inputPath, outputPath);
    return 0;
  }


  // Main
  ///////////////////////////////////////////////////////////////////////////	

  public static void main(String[] args) throws Exception 
  {
    int res = ToolRunner.run(new Configuration(), new FastqPreprocessorAvroCompressed(), args);
    System.exit(res);
  }
}

