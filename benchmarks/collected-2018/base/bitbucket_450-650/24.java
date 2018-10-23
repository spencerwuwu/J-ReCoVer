// https://searchcode.com/api/result/125457342/

package contrail;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.List;
import java.util.Map;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.FileInputFormat;
import org.apache.hadoop.mapred.FileOutputFormat;
import org.apache.hadoop.mapred.JobClient;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.MapReduceBase;
import org.apache.hadoop.mapred.Mapper;
import org.apache.hadoop.mapred.OutputCollector;
import org.apache.hadoop.mapred.Reducer;
import org.apache.hadoop.mapred.Reporter;
import org.apache.hadoop.mapred.RunningJob;
import org.apache.hadoop.mapred.TextInputFormat;
import org.apache.hadoop.mapred.TextOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.apache.log4j.Logger;


public class BuildGraph extends Configured implements Tool 
{	
	private static final Logger sLogger = Logger.getLogger(BuildGraph.class);

	private static class BuildGraphMapper extends MapReduceBase 
	implements Mapper<LongWritable, Text, Text, Text> 
	{
		private static int K = 0;
		private static int TRIM5 = 0;
		private static int TRIM3 = 0;

		public void configure(JobConf job) 
		{
			K = Integer.parseInt(job.get("K"));
			TRIM5 = Integer.parseInt(job.get("TRIM5"));
			TRIM3 = Integer.parseInt(job.get("TRIM3"));
		}

		/*
		 * Input (int,text) - Each input tuple is the linumber of the file the input was read from and the value is 
		 * 	is the text of the line. The text is a tab delimted sring where the first field is some form
		 * 	of tag and the second field is the sequence of the read.
		 * 
		 * Output (text,text) - The key of the output tuple is the canonical version of the K-mer representing
		 * 	an edge in the DeBruijn graph. The k-mer is represented as compressed version of the DNA sequence. 
		 *  value - A tab delimited string containing the following fields:
		 *		field 0 - a two character sequence e.g "ff", "fr", where 
		 *          each character specifies the canonical direction of one of the kmers.
		 *      field 1 - is the last base of the second k-mer.
		 *      	In conjuction with the key, this allows us to construct the two k-1 Mer nodes which
		 *        	are connected by the K-mer encoded in the key. 
		 *		field 2 -  is "tagCchunknum" where tag is the ID for the read from which this output tuple
		 *			was generated. chunknum is how many times our mapper saw the canonical version of this k-mer
		 *			in this read. If the read is only seen once then this field is just tag. 
		 *      field 3 - state. Values are "5", or "m" I think 5 means 5 end of the read
		 *               whereas "m" means internal read  	
		 *
		 * For each successive pair of k-mers in the input read, we output two output tuples; where each tuple corresponds to the
		 * read coming from a different strand of the pair. 
		 * 
		 * (non-Javadoc)
		 * @see org.apache.hadoop.mapred.Mapper#map(java.lang.Object, java.lang.Object, org.apache.hadoop.mapred.OutputCollector, org.apache.hadoop.mapred.Reporter)
		 */
		public void map(LongWritable lineid, Text nodetxt,
				OutputCollector<Text, Text> output, Reporter reporter)
						throws IOException 
						{
			String[] fields = nodetxt.toString().split("\t");

			if (fields.length != 2)
			{
				//System.err.println("Warning: invalid input: \"" + nodetxt.toString() + "\"");
				reporter.incrCounter("Contrail", "input_lines_invalid", 1);
				return;
			}

			String tag = fields[0];
			String seq = fields[1].toUpperCase();

			// Hard chop a few bases off of each end of the read
			if (TRIM5 > 0 || TRIM3 > 0)
			{
				// System.err.println("orig: " + seq);
				seq = seq.substring(TRIM5, seq.length() - TRIM5 - TRIM3);
				// System.err.println("trim: " + seq);
			}

			// Automatically trim Ns off the very ends of reads
			int endn = 0;
			while (endn < seq.length() && seq.charAt(seq.length()-1-endn) == 'N') { endn++; }
			if (endn > 0) { seq = seq.substring(0, seq.length()-endn); }

			int startn = 0;
			while (startn < seq.length() && seq.charAt(startn) == 'N') { startn++; }
			if (startn > 0) { seq = seq.substring(startn); }

			// Check for non-dna characters
			if (seq.matches(".*[^ACGT].*"))
			{
				//System.err.println("WARNING: non-DNA characters found in " + tag + ": " + seq);
				reporter.incrCounter("Contrail", "reads_skipped", 1);	
				return;
			}

			// check for short reads
			if (seq.length() <= K)
			{
				//System.err.println("WARNING: read " + tag + " is too short: " + seq);
				reporter.incrCounter("Contrail", "reads_short", 1);	
				return;
			}

			// Now emit the edges of the de Bruijn Graph
			// (Lewi) For each read, each successive kmer is a node in the graph
			//        and the edge connecting them is the (k-1) kmer of overlap.
			char ustate = '5';
			char vstate = 'i';

			Set<String> seenmers = new HashSet<String>();

			String chunkstr = "";
			int chunk = 0;

			int end = seq.length() - K;

			for (int i = 0; i < end; i++)
			{
				// u and v are sequential K-mers in the
				// read.
				String u = seq.substring(i,   i+K);
				String v = seq.substring(i+1, i+1+K);

				// f is the first base in u
				// l is the last base in l
				// f,l correspond to the non-overlap regions of u,v
				String f = seq.substring(i, i+1);
				String l = seq.substring(i+K, i+K+1);
				f = Node.rc(f);

				// ud, vd are characters representing
				// the canonical direction of the two kmers
				char ud = Node.canonicaldir(u);
				char vd = Node.canonicaldir(v);

				String t  = Character.toString(ud) + vd;
				String tr = Node.flip_link(t);

				String uc0 = Node.canonicalseq(u);
				String vc0 = Node.canonicalseq(v);

				// Keys of the output will be 
				// the k-mer sequences
				String uc = Node.str2dna(uc0);
				String vc = Node.str2dna(vc0);

				//System.out.println(u + " " + uc0 + " " + ud + " " + uc);
				//System.out.println(v + " " + vc0 + " " + vd + " " + vc);

				if ((i == 0) && (ud == 'r'))  { ustate = '6'; }
				if (i+1 == end) { vstate = '3'; }

				boolean seen = (seenmers.contains(u) || seenmers.contains(v) || u.equals(v));
				seenmers.add(u);

				if (seen)
				{
					chunk++;
					chunkstr = "c" + chunk;
					//#print STDERR "repeat internal to $tag: $uc u$i $chunk\n";
				}

				//(Lewi) tag is the first field after splitting the value of the input key.
				//       It looks like tag is some kind of identifier (i.e. similar to line number)
				//       Chunstr is a count of how many times this mapper has seen this kmer.
				//       t - is a two character sequence e.g "ff", "fr", where 
				//           each character specificies the canonical direction of one of the kmers.
				//       l - is the last base of the second k-mer.
				//       tag - is the tag for the seqeunce. In the
				//       chunkstr - Counts how many times this mapper has seen this kmer.
				//       ustate - Values are "5", or "m" I think 5 means 5 end of the read
				//                whereas "m" means internal read
				output.collect(new Text(uc), 
						new Text(t + "\t" + l + "\t" + tag + chunkstr + "\t" + ustate));

				if (seen)
				{
					chunk++;
					chunkstr = "c" + chunk;
					//#print STDERR "repeat internal to $tag: $vc v$i $chunk\n";
				}

				//print "$vc\t$tr\t$f\t$tag$chunk\t$vstate\n";

				output.collect(new Text(vc), 
						new Text(tr + "\t" + f + "\t" + tag + chunkstr + "\t" + vstate));

				ustate = 'm';
			}

			reporter.incrCounter("Contrail", "reads_good", 1);
			reporter.incrCounter("Contrail", "reads_goodbp", seq.length());
						}			
	}

	private static class BuildGraphReducer extends MapReduceBase 
	implements Reducer<Text, Text, Text, Text> 
	{
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

		/**
		 * Perform the reduce phase.
		 * 
		 * Input (edge,value) - Input key is a compressed representation of the K-mer representing an edge.
		 * 	The value is a tab delimeted field providing information about the edge and the two k-1 mers it connects.
		 * For more info see the javadoc for the mappers.  
		 * 
		 * Output (kmer,node)
		 * 	key - The key is a compressed version of the K-Mer representing a source Node.
		 *  node - A serialized version of a Node object representing the source node. This object contains
		 *    all of the outgoing edges for this node. 
		 *  
		 */
		public void reduce(Text curnode, Iterator<Text> iter,
				OutputCollector<Text, Text> output, Reporter reporter)
						throws IOException 
						{
			Node node = new Node();

			String mertag = null;
			float cov = 0;

			// The keys for edges map are the two characte type code,
			// e.g "ff", "rf", "rr","fr" assigned to each edge based 
			// on the canonical direction of the read. 
			//
			// The values are a MAP contain the neighbors for this read. 
			// The keys of the negbors are the DNA character e.g "A" representing
			// the base we need to add to the K-Mer representing this node so that
			// we can construct the two k Mers connected by the k-1 Mer of overlap 
			// where the outgoing node is represented by currnode
			Map<String, Map<String, List<String>>> edges = new HashMap<String, Map<String, List<String>>>();

			// loop over all the tuples for this kmer. Since they keys, are the same.
			// The k-1 Mer representing the starting node and the K-mer representing the edge are the same. 
			while(iter.hasNext())
			{
				String valstr = iter.next().toString();
				String [] vals = valstr.split("\t");

				String type     = vals[0]; // edge type between mers
				String neighbor = vals[1]; // id of neighboring node
				String tag      = vals[2]; // id of read contributing to edge
				String state    = vals[3]; // internal or end mer

				// Add the edge to the neighbor
				Map<String, List<String>> neighborinfo = null;
				if (edges.containsKey(type))
				{
					neighborinfo = edges.get(type);
				}
				else
				{
					neighborinfo = new HashMap<String, List<String>>();
					edges.put(type, neighborinfo);
				}


				// Now record the read supports the edge
				List<String> tags = null;
				if (neighborinfo.containsKey(neighbor))
				{
					tags = neighborinfo.get(neighbor);
				}
				else
				{
					tags = new ArrayList<String>();
					neighborinfo.put(neighbor, tags);
				}

				if (tags.size() < MAXTHREADREADS)
				{
					tags.add(tag);
				}

				// Check on the mertag
				// set mertag to the smallest (lexicographically) tag 
				// of all the tags associated with this key
				if (mertag == null || (tag.compareTo(mertag) < 0))
				{
					mertag = tag;
				}

				// Update coverage, offsets
				if (!state.equals("i"))
				{
					cov++;

					if (state.equals("6"))
					{
						node.addR5(tag, K-1, 1, MAXR5);
					}
					else if (state.equals("5"))
					{
						node.addR5(tag, 0, 0, MAXR5);
					}
				}
			}

			node.setMertag(mertag);
			node.setCoverage(cov);

			// Decode the key for the input tuple into a DNA sequence.
			String seq = Node.dna2str(curnode.toString());
			String rc  = Node.rc(seq);

			// create a node representing the k-mer corresponding 
			// to the key
			node.setstr_raw(curnode.toString());

			// extract the k-1 Mers corresponding the overlap
			// between the KMers
			seq = seq.substring(1);
			rc  = rc.substring(1);

			char [] dirs = {'f', 'r'};

			for (int d = 0; d < 2; d++)
			{
				String x = Character.toString(dirs[d]); 

				int degree = 0;

				for (int e = 0; e < 2; e++)
				{
					String t = x + dirs[e];

					if (edges.containsKey(t))
					{
						degree += edges.get(t).size();
					}
				}

				for(int e = 0; e < 2; e++)
				{
					String t = x + dirs[e];

					if (edges.containsKey(t))
					{
						Map<String, List<String>> edgeinfo = edges.get(t);

						for (String vc : edgeinfo.keySet())
						{
							String v = seq;
							if (dirs[d] == 'r') { v = rc; }

							// Construct the k-Mer corresponding to the destination
							// node for this edge. seq is the K-1 Mer of overlap.
							// vc (the keys of edgeinfo) are the base we need to add
							// to the k-1 overlap to get the destination kmer							
							v = v +  vc;

							if (dirs[e] == 'r') { v = Node.rc(v); }

							String link = Node.str2dna(v);

							node.addEdge(t, link);

							if ((degree > 1) || RECORD_ALL_THREADS)
							{
								for (String r : edgeinfo.get(vc))
								{
									node.addThread(t, link, r);
								}
							}
						}
					}
				}
			}

			output.collect(curnode, new Text(node.toNodeMsg()));
			reporter.incrCounter("Contrail", "nodecount", 1);
						}
	}



	public RunningJob run(String inputPath, String outputPath) throws Exception
	{
		sLogger.info("Tool name: BuildGraph");
		sLogger.info(" - input: "  + inputPath);
		sLogger.info(" - output: " + outputPath);

		JobConf conf = new JobConf(BuildGraph.class);
		conf.setJobName("BuildGraph " + inputPath + " " + ContrailConfig.K);

		ContrailConfig.initializeConfiguration(conf);

		FileInputFormat.addInputPath(conf, new Path(inputPath));
		FileOutputFormat.setOutputPath(conf, new Path(outputPath));

		conf.setInputFormat(TextInputFormat.class);
		conf.setOutputFormat(TextOutputFormat.class);

		conf.setMapOutputKeyClass(Text.class);
		conf.setMapOutputValueClass(Text.class);

		conf.setOutputKeyClass(Text.class);
		conf.setOutputValueClass(Text.class);

		conf.setMapperClass(BuildGraphMapper.class);
		conf.setReducerClass(BuildGraphReducer.class);

		//delete the output directory if it exists already
		FileSystem.get(conf).delete(new Path(outputPath), true);

		return JobClient.runJob(conf);
	}

	public int run(String[] args) throws Exception 
	{

		// TODO (jlewi): Currently the options specified here need to appear first on
		// the command line otherwise we get an exeption when calling CommandLineParser.parse
		// What we'd like to have happen is that any options not defined here
		// get stored in CommandLine.args which get passed onto ContrailConfig.parseOptions.
		Option kmer = new Option("k","k",true,"k. The length of each kmer to use.");
		Option input = new Option("input","input",true,"The directory containing the input (i.e the output of BuildGraph.)");
		Option output = new Option("output","output",true,"The directory where the output should be written to.");

		Options options = new Options();
		options.addOption(kmer);
		options.addOption(input);
		options.addOption(output);

		CommandLineParser parser = new GnuParser();
		CommandLine line = parser.parse( options, args, true );


		if (!line.hasOption("input") || !line.hasOption("output") || !line.hasOption("k")){
			System.out.println("ERROR: Missing required arguments");
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp( "QuickMerge", options );
		}

		String inputPath  = line.getOptionValue("input");
		String outputPath = line.getOptionValue("output");
		ContrailConfig.K = Integer.parseInt(line.getOptionValue("k"));



		ContrailConfig.parseOptions(line.getArgs());
		long starttime = System.currentTimeMillis();

		run(inputPath, outputPath);

		long endtime = System.currentTimeMillis();

		float diff = (float) (((float) (endtime - starttime)) / 1000.0);

		System.out.println("Runtime: " + diff + " s");

		return 0;
	}

	public static void main(String[] args) throws Exception 
	{
		int res = ToolRunner.run(new Configuration(), new BuildGraph(), args);
		System.exit(res);
	}
}

