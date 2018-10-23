// https://searchcode.com/api/result/125457359/

package contrail.avro;
import contrail.CompressedRead;
import contrail.ContrailConfig;
import contrail.GraphNodeData;
import contrail.KMerEdge;
import contrail.ReadState;
import contrail.sequences.Alphabet;
import contrail.sequences.DNAAlphabetFactory;
import contrail.sequences.DNAUtil;
import contrail.sequences.Sequence;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.avro.mapred.AvroCollector;
import org.apache.avro.mapred.AvroJob;
import org.apache.avro.mapred.AvroMapper;
import org.apache.avro.mapred.AvroReducer;
import org.apache.avro.mapred.Pair;
import org.apache.avro.Schema;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapred.FileInputFormat;
import org.apache.hadoop.mapred.FileOutputFormat;
import org.apache.hadoop.mapred.JobClient;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.Reporter;
import org.apache.hadoop.mapred.RunningJob;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.apache.log4j.Logger;


public class BuildGraphAvro extends Stage
{	
	private static final Logger sLogger = Logger.getLogger(BuildGraphAvro.class);

	public static final Schema kmer_edge_schema = (new KMerEdge()).getSchema();
	public static final Schema graph_node_data_schema = 
	    (new GraphNodeData()).getSchema();
	
	/**
	 * Define the schema for the mapper output. The keys will be a byte buffer
	 * representing the compressed source KMer sequence. The value will be an 
	 * instance of KMerEdge.
	 */
	public static final Schema MAP_OUT_SCHEMA = 
	    Pair.getPairSchema(Schema.create(Schema.Type.BYTES), kmer_edge_schema);
	
	/**
	 * Define the schema for the reducer output. The keys will be a byte buffer
	 * representing the compressed source KMer sequence. The value will be an 
	 * instance of GraphNodeData. 
	 */
	public static final Schema REDUCE_OUT_SCHEMA = 
	    Pair.getPairSchema(Schema.create(Schema.Type.BYTES), graph_node_data_schema);
	
	
	protected void initializeDefaultOptions() {
	  super.initializeDefaultOptions();
	  default_options.put("TRIM3", new Long(0));
	  default_options.put("TRIM5", new Long(0));
	  default_options.put("MAXR5", new Long(250));
	  default_options.put("MAXTHREADREADS", new Long(250));
	  default_options.put("RECORD_ALL_THREADS", new Long(0));
	}
	/**
	 * Get the options required by this stage.
	 */
	protected List<Option> getCommandLineOptions() {
	  List<Option> options = super.getCommandLineOptions();
	  
	  // Default values.
	  // hard trim
	  long TRIM3 = (Long)default_options.get("TRIM3");
	  long TRIM5 = (Long)default_options.get("TRIM5");
	  long MAXR5 = (Long)default_options.get("MAXR5");
	  long MAXTHREADREADS = (Long)default_options.get("MAXTHREADREADS");
	  long  RECORD_ALL_THREADS = (Long)default_options.get("RECORD_ALL_THREADS");
	  // Add options specific to this stage.
    options.add(OptionBuilder.withArgName("k").hasArg().withDescription(
        "Graph nodes size [required]").create("k"));
    options.add(OptionBuilder.withArgName(
        "max reads").hasArg().withDescription(
            "max reads starts per node (default: " + MAXR5 +")").create(
                "maxr5"));
    options.add(OptionBuilder.withArgName("3' bp").hasArg().withDescription(
        "Chopped bases (default: " + TRIM3 +")").create("trim3"));
    options.add(OptionBuilder.withArgName("5' bp").hasArg().withDescription(
        "Chopped bases (default: " + TRIM5 + ")").create("trim5"));
    options.add(new Option(
        "record_all_threads",  "record threads even on non-branching nodes"));
    
    options.addAll(ContrailOptions.getInputOutputPathOptions());
    return options;
	}
	/**
	 * This class contains the operations for preprocessing sequences.
	 * 
	 * This object is instantiated once for each mapper and customizes the operations
	 * based on the settings and the alphabet.
	 * 
	 * We use a separate class so that its easy to unittest.
	 */
	public static class SequencePreProcessor {

	  private Alphabet alphabet;
	  private int trim5;
	  private int trim3;
	    
	  private boolean hasTrimVal;
	  private int trimVal;
	  
	  /**
    * @param alphabet - The alphabet. 
    * @param trim5 - Number of bases to trim off the start
    * @param trim3 - Number of bases to trim off the end.
    */	  
	  public SequencePreProcessor(Alphabet alphabet, int trim5, int trim3) {
	    this.alphabet = alphabet;
	    this.trim5 = trim5;
	    this.trim3 = trim3;

	    // Check if this alphabet has a character which should be removed from 
	    // both ends of the sequence.
	    if (alphabet.hasLetter('N')) {
	      hasTrimVal = true;
	      trimVal = alphabet.letterToInt('N');
	    } 
	    else {
	      hasTrimVal = false;
	    }
	  }
	   /**
     * Pre process a sequence.
     * 
     * @param seq - The sequence to process
     * @return - The processed sequence.
     */
    public Sequence PreProcess(Sequence seq) {
      // Start and end will store the valid range of the sequence.
      int start = 0;
      int end = seq.size();
          
      // Hard chop a few bases off each end of the read
      if (trim5 > 0 || trim3 > 0) {
        start = trim5;
        end = seq.capacity() - trim3;          
      }

      // Automatically trim Ns off the very ends of reads     
      if (hasTrimVal) {
        while (end > 0 && seq.valAt(end) == trimVal) { 
          end--; 
        }
               
        while (start < seq.size() && seq.valAt(start) == trimVal) { 
          start++;
        }
      } 
      return seq.subSequence(start, end);
    }
	}

	 /**
   * Mapper for BuildGraph.
   * Class is public to facilitate unit-testing.
   */
	public static class BuildGraphMapper extends 
	    AvroMapper<CompressedRead, Pair<ByteBuffer, KMerEdge>> 	  
	{
		private static int K = 0;

		private Alphabet alphabet = DNAAlphabetFactory.create();
		private Sequence seq = new Sequence(DNAAlphabetFactory.create());
		private SequencePreProcessor preprocessor;
		
		private KMerEdge node = new KMerEdge();
		public void configure(JobConf job) 
		{
			K = Integer.parseInt(job.get("K"));
			if (K <= 0) {
			  throw new RuntimeException("K must be a positive integer");
			}
			int TRIM5 = Integer.parseInt(job.get("TRIM5"));
			int TRIM3 = Integer.parseInt(job.get("TRIM3"));
					
			preprocessor = new SequencePreProcessor(alphabet, TRIM5, TRIM3); 
		}
	  
 
		/*
		 * Input (CompressedRead) - Each input is an instance of CompressedRead. 
		 *   
		 * Output (ByteBuffer, KMerEdge): The output key is a sequence of bytes
		 *   representing the compressed KMer for the source node. The value
		 *   is an instance of KMerEdge which contains all the information
		 *   for an edge originating from this source KMer.
		 *
		 * For each successive pair of k-mers in the read, we output two 
		 * tuples; where each tuple corresponds to the read coming from a different 
		 * strand of the sequence. 
		 */
		@Override
		public void map(CompressedRead compressed_read, 
		    AvroCollector<Pair<ByteBuffer, KMerEdge>> output, Reporter reporter)
						throws IOException {

		  seq.readPackedBytes(compressed_read.getDna().array(), 
		                      compressed_read.getLength());		  
		  seq = preprocessor.PreProcess(seq);

			// Check for short reads.
			if (seq.size() <= K)
			{
				reporter.incrCounter("Contrail", "reads_short", 1);	
				return;
			}

			ReadState ustate = ReadState.END5;
			ReadState vstate = ReadState.I;

			Set<String> seenmers = new HashSet<String>();
			
			int chunk = 0;

			int end = seq.size() - K;

	    // Now emit the edges of the de Bruijn Graph.
      // Each successive kmer in the read is a node in the graph
      // and the edge connecting them is the (k-1) of overlap.
			// Since we don't know which strand the read came from, we need
			// to consider both the read and its reverse complement when generating
			// edges. 
			for (int i = 0; i < end; i++)
			{
				// ukmer and vkmer are sequential KMers in the read.
			  Sequence ukmer = seq.subSequence(i, i+K);
			  Sequence vkmer = seq.subSequence(i+1, i+1+K);
			  
			  // ukmer_start and vkmer_end are the base we need to add 
			  // to the source kmer in order to generate the destination KMer.
			  Sequence ukmer_start = seq.subSequence(i, i+1);
			  Sequence vkmer_end = seq.subSequence(i+K, i+K+1);			  
			  ukmer_start = DNAUtil.reverseComplement(ukmer_start);

	      // Construct the canonical representation of each kmer.
        // This ensures that a kmer coming from the forward and reverse
        // strand are both represented using the same node.
        Sequence ukmer_canonical = DNAUtil.canonicalseq(ukmer);
        Sequence vkmer_canonical = DNAUtil.canonicalseq(vkmer);
        
				// The canonical direction of the two kmers.
        char ukmer_dir;
        char vkmer_dir;
        if (ukmer_canonical.equals(ukmer)) {
          ukmer_dir = 'f';
        } else {
          ukmer_dir = 'r';
        }
			  if (vkmer_canonical.equals(vkmer)) {
			    vkmer_dir = 'f';
			  } else {
			    vkmer_dir = 'r';
			  }

			  String link_dir = Character.toString(ukmer_dir) + vkmer_dir;
			  String reverse_link = DNAUtil.flip_link(link_dir);
			  			  
				if ((i == 0) && (ukmer_dir == 'r'))  { ustate = ReadState.END6; }
				if (i+1 == end) { vstate = ReadState.END3; }

				// TODO(jlewi): It would probably be more efficient not to use a string
				// representation of the Kmers in seen.				
        boolean seen = (seenmers.contains(ukmer.toString()) || 
                        seenmers.contains(vkmer.toString()) || 
                        ukmer.equals(vkmer));
        seenmers.add(ukmer.toString());
				if (seen)
				{
					chunk++;
				}

				// Output an edge assuming we are reading the forward strand.
				{
				  Pair<ByteBuffer, KMerEdge> pair = 
				      new Pair<ByteBuffer, KMerEdge>(MAP_OUT_SCHEMA);
				  ByteBuffer key;

				  // TODO(jlewi): Should we verify that all unset bits in node.kmer are
				  // 0?
				  node.setLinkDir(link_dir);
				  node.setLastBase(ByteBuffer.wrap(vkmer_end.toPackedBytes(), 0, vkmer_end.numPackedBytes()));
				  node.setTag(compressed_read.getId());
				  node.setState(ustate);
				  node.setChunk(chunk);
				  key = ByteBuffer.wrap(ukmer_canonical.toPackedBytes(), 0, 
				      ukmer_canonical.numPackedBytes());
				  pair.set(key, node);
				  output.collect(pair);
				}
				if (seen)
				{
					chunk++;
				}

				{
				  Pair<ByteBuffer, KMerEdge> pair = 
				      new Pair<ByteBuffer, KMerEdge>(MAP_OUT_SCHEMA);
				  ByteBuffer key;				
				  // Output an edge assuming we are reading the reverse strand.
				  // TODO(jlewi): Should we verify that all unset bits in node.kmer are
				  // 0?
				  node.setLinkDir(reverse_link);
				  node.setLastBase(ByteBuffer.wrap(ukmer_start.toPackedBytes(), 0, ukmer_start.numPackedBytes()));
				  node.setTag(compressed_read.id);
				  node.setState(vstate);
				  node.setChunk(chunk);
				  key = ByteBuffer.wrap(vkmer_canonical.toPackedBytes(), 0, 
				      vkmer_canonical.numPackedBytes());
				  pair.set(key, node);
				  output.collect(pair);
				}
				ustate = ReadState.MIDDLE;
			}

			reporter.incrCounter("Contrail", "reads_good", 1);
			reporter.incrCounter("Contrail", "reads_goodbp", seq.size());
		}			
	}

	/**
	 * Reducer for BuildGraph.
	 * 
	 * The reducer outputs a set of key value pairs where the key is a sequence
	 * of bytes representing the compressed source KMer. The value is a 
	 * GraphNodeData datum which contains all the information about edges from
	 * the source KMer to different destination KMers.
	 * 
	 * This class is public to facilitate unit-testing.
	 */
	public static class BuildGraphReducer extends 
	    AvroReducer<ByteBuffer, KMerEdge, Pair<ByteBuffer, GraphNodeData>> {
		private static int K = 0;
		private static int MAXTHREADREADS = 0;
		private static int MAXR5 = 0;
		private static boolean RECORD_ALL_THREADS = false;

		public void configure(JobConf job) {
			K = Integer.parseInt(job.get("K"));
			MAXTHREADREADS = Integer.parseInt(job.get("MAXTHREADREADS"));
			MAXR5 = Integer.parseInt(job.get("MAXR5"));
			RECORD_ALL_THREADS = Integer.parseInt(job.get("RECORD_ALL_THREADS")) == 1;
		}
		
		@Override
		public void reduce(ByteBuffer source_kmer_packed_bytes, Iterable<KMerEdge> iterable,
				AvroCollector<Pair<ByteBuffer, GraphNodeData>> collector, Reporter reporter)
						throws IOException {		  
		  Alphabet alphabet = DNAAlphabetFactory.create();		  
		  GraphNode graphnode = new GraphNode();
		  graphnode.setCanonicalSourceKMer(source_kmer_packed_bytes, K);
		  
		  Sequence canonical_src = new Sequence(alphabet);
		  canonical_src.readPackedBytes(source_kmer_packed_bytes.array(), K);
		  		  
		  KMerReadTag mertag = null;
		  int cov = 0;

		  Iterator<KMerEdge> iter = iterable.iterator();
			// Loop over all KMerEdges which originate with the KMer represented by
			// the input key. Since the source and destination KMers overlap for K-1
			// bases, we can construct the destination KMer using the last K-1 bases 
			// of the source and the additional base for the destination stored in 
		  // KMerEdge. 
			while(iter.hasNext()) {
			  KMerEdge edge = iter.next();
			  Sequence last_base = new Sequence(alphabet);

			  CharSequence edge_dir = edge.getLinkDir();			    

			  last_base.readPackedBytes(edge.getLastBase().array(), 1);
			  
			  String read_id = edge.getTag().toString();			   			  
			  KMerReadTag tag = new KMerReadTag(read_id, edge.getChunk());

			  Sequence dest;
			  
			  if (edge_dir.charAt(0) == 'f') {
			    dest = canonical_src.subSequence(1, K);
			  } else {
			    dest = DNAUtil.reverseComplement(canonical_src).subSequence(1, K);
			  }
			  dest.add(last_base);
			  
			  Sequence canonical_dest = DNAUtil.canonicalseq(dest);
			  
			  // Set mertag to the smallest (lexicographically) tag 
			  // of all the tags associated with this key
			  if (mertag == null || (tag.compareTo(mertag) < 0)) {
			    mertag = tag;
			  }

			  ReadState state = edge.getState(); 
			  // Update coverage and offsets.
			  if (state != ReadState.I) {
			    cov++;
			    if (state == ReadState.END6) {
			      graphnode.addR5(tag, K-1, true, MAXR5);
			    } else if (state == ReadState.END5) {
			      graphnode.addR5(tag, 0, false, MAXR5);
			    }
			  }
			  // Add an edge to this destination.
			  graphnode.addEdge(edge.getLinkDir(), canonical_dest, tag.toString(), 
			                    MAXTHREADREADS);
			}

			graphnode.setMertag(mertag);
			graphnode.setCoverage(cov);

			Pair<ByteBuffer, GraphNodeData> pair = 
			    new Pair<ByteBuffer, GraphNodeData>(REDUCE_OUT_SCHEMA);
			pair.set(source_kmer_packed_bytes, graphnode.getData());
			collector.collect(pair);
			reporter.incrCounter("Contrail", "nodecount", 1);
		}
	}

  protected void parseCommandLine(CommandLine line) {
    super.parseCommandLine(line);       
    
    if (line.hasOption("k")) {
      stage_options.put("K", Long.valueOf(line.getOptionValue("k"))); 
    }    
    if (line.hasOption("maxr5")) { 
      stage_options.put("MAXR5", Long.valueOf(line.getOptionValue("maxr5"))); 
    }
    if (line.hasOption("trim3")) { 
      stage_options.put("TRIM3", Long.valueOf(line.getOptionValue("trim3"))); 
    }
    if (line.hasOption("trim5")) { 
      stage_options.put("TRIM5", Long.valueOf(line.getOptionValue("trim5"))); 
    }
    if (line.hasOption("inputpath")) { 
      stage_options.put("inputpath", line.getOptionValue("inputpath")); 
    }
    if (line.hasOption("outputpath")) { 
      stage_options.put("outputpath", line.getOptionValue("outputpath")); 
    }
  }
	
	public int run(String[] args) throws Exception 
	{
	  sLogger.info("Tool name: BuildGraph");
	  parseCommandLine(args);	  
	  return run();
	}
	
	@Override
	protected int run() throws Exception {  
	  String inputPath = (String) stage_options.get("inputpath");
	  String outputPath = (String) stage_options.get("outputpath");
	  long K = (Long)stage_options.get("K");
	  
    sLogger.info(" - input: "  + inputPath);
    sLogger.info(" - output: " + outputPath);

    JobConf conf = new JobConf(BuildGraphAvro.class);
    conf.setJobName("BuildGraph " + inputPath + " " + K);

    initializeJobConfiguration(conf);

    FileInputFormat.addInputPath(conf, new Path(inputPath));
    FileOutputFormat.setOutputPath(conf, new Path(outputPath));

    CompressedRead read = new CompressedRead();
    AvroJob.setInputSchema(conf, read.getSchema());
    AvroJob.setMapOutputSchema(conf, BuildGraphAvro.MAP_OUT_SCHEMA);
    AvroJob.setOutputSchema(conf, BuildGraphAvro.REDUCE_OUT_SCHEMA);
    
    AvroJob.setMapperClass(conf, BuildGraphMapper.class);
    AvroJob.setReducerClass(conf, BuildGraphReducer.class);

    // Delete the output directory if it exists already
    Path out_path = new Path(outputPath);
    if (FileSystem.get(conf).exists(out_path)) {
      // TODO(jlewi): We should only delete an existing directory
      // if explicitly told to do so.
      sLogger.info("Deleting output path: " + out_path.toString() + " " + 
                   "because it already exists.");       
      FileSystem.get(conf).delete(out_path, true);  
    }
    
		long starttime = System.currentTimeMillis();		
		JobClient.runJob(conf);
		long endtime = System.currentTimeMillis();

		float diff = (float) (((float) (endtime - starttime)) / 1000.0);

		System.out.println("Runtime: " + diff + " s");
		return 0;
	}
	
	public static void main(String[] args) throws Exception 
	{
		int res = ToolRunner.run(new Configuration(), new BuildGraphAvro(), args);
		System.exit(res);
	}
}

