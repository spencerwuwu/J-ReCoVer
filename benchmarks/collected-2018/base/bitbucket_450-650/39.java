// https://searchcode.com/api/result/125457345/

package contrail;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

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


public class QuickMerge extends Configured implements Tool 
{	
	private static final Logger sLogger = Logger.getLogger(QuickMerge.class);

	private static class QuickMergeMapper extends MapReduceBase 
	implements Mapper<LongWritable, Text, Text, Text> 
	{
		private static Node node = new Node();

		/**
		 * Mapper for QuickMerge.
		 * 
		 * Input is read from a text file where each line represents the
		 * string encoding of a node in the graph; (for example, the output 
		 * produced by BuildGraph). 
		 * 
		 * For each input tuple we output a tuple (node mertag, node encoded string).
		 * 
		 */
		public void map(LongWritable lineid, Text nodetxt,
				OutputCollector<Text, Text> output, Reporter reporter)
						throws IOException 
						{
			node.fromNodeMsg(nodetxt.toString());

			String mertag = node.getMertag();
			output.collect(new Text(mertag), new Text(node.toNodeMsg(true)));
			reporter.incrCounter("Contrail", "nodes", 1);	
						}
	}

	private static class QuickMergeReducer extends MapReduceBase
	/**
	 *  The reducer.
	 *
	 */
	implements Reducer<Text, Text, Text, Text> 
	{
		private static int K = 0;
		public static boolean VERBOSE = false;

		public void configure(JobConf job) {
			K = Integer.parseInt(job.get("K"));
		}

		/**
		 * Reducer for QuickMerge.
		 * 
		 * Input:
		 * The key is a mertag. The value is a list of the serialized nodes with that mertag.
		 * 
		 */
		public void reduce(Text mertag, Iterator<Text> iter,
				OutputCollector<Text, Text> output, Reporter reporter)
						throws IOException 
						{			
			int saved    = 0;
			int chains   = 0;
			int cchains  = 0;
			int totallen = 0;

			String DONE = "DONE";			

			// Load the nodes with the same key (mertag) into memory
			Map<String, Node> nodes = new HashMap<String, Node>();

			//VERBOSE = mertag.toString().equals("32_15326_10836_0_2") || mertag.toString().equals("22_5837_4190_0_2") || mertag.toString().equals("101_8467_7940_0_2");

			while(iter.hasNext())
			{
				Node node = new Node();
				String nodestr = iter.next().toString();

				if (VERBOSE)
				{
					//System.err.println(mertag + " " + nodestr);
				}

				if ((nodes.size() > 10) && (nodes.size() % 10 == 0))
				{
					//System.err.println("Common mer: " + mertag.toString() + " cnt:" + nodes.size());
				}

				node.fromNodeMsg(nodestr);
				nodes.put(node.getNodeId(), node);
			}

			//if (nodes.size() > 10) { System.err.println("Loaded all nodes: " + nodes.size() + "\n"); }

			// Now try to merge each node

			int donecnt = 0;
			for (String nodeid : nodes.keySet())
			{
				donecnt++;
				//if ((donecnt % 10) == 0) { System.err.println("Completed Merges: " + donecnt + "\n"); }

				Node node = nodes.get(nodeid);

				// The done field is used to keep track whether we've already processed the
				// node with this id. 
				if (node.hasCustom(DONE)) { continue; }
				node.setCustom(DONE, "1");

				// {r1,r2} >> rtail -> c1 -> c2 -> c3 -> node -> c4 -> c5 -> c6 -> ftail >> {f1,f2}

				TailInfo rtail = TailInfo.find_tail(nodes, node, "r");
				Node rtnode = nodes.get(rtail.id);

				// catch cycles by looking for the ftail from rtail, not node
				TailInfo ftail = TailInfo.find_tail(nodes, rtnode, rtail.dir); 
				Node ftnode = nodes.get(ftail.id);

				rtnode.setCustom(DONE, "1");
				ftnode.setCustom(DONE, "1");

				int chainlen = 1 + ftail.dist;

				chains++;
				totallen += chainlen;

				//VERBOSE = rtail.id.equals("HRJMRHHETMRJHSF") || rtail.id.equals("EECOECEOEECOECA") || rtail.id.equals("ECOECEOEECOECEK");
				if (VERBOSE) { System.err.print(nodeid + " " + chainlen + " " + rtail + " " + ftail + " " + mertag.toString()); }
				
				// domerge indicates whether we can merge the nodes.
				// domerge = 0  default/initial value
				// domerge = 1  chainlen >2 but allinmemory= false
				// domerge = 2  allinmemory = true
				int domerge = 0;
				if (chainlen > 1)
				{
					boolean allinmemory = true;

					for(String et : Node.edgetypes)
					{
						List<String> e = rtnode.getEdges(et);
						if (e != null)
						{
							for(String v : e)
							{
								if (!nodes.containsKey(v))
								{
									allinmemory = false;
									break;
								}
							}
						}
					}

					if (allinmemory)       { domerge = 2; }
					else if (chainlen > 2) { domerge = 1; }
				}

				if (VERBOSE) { System.err.println(" domerge=" + domerge); }

				if (domerge > 0)
				{
					chainlen--; // Replace the chain with 1 ftail
					if (domerge == 1) { chainlen--; } // Need rtail too

					// start at the rtail, and merge until the ftail

					if (VERBOSE) 
					{ 
						System.err.println("[==");
						System.err.println(rtnode.toNodeMsg(true));
					}

					// mergedir is the direction to merge relative to rtail
					String mergedir = rtail.dir;

					TailInfo first = rtnode.gettail(mergedir);
					Node firstnode = nodes.get(first.id);

					// quick sanity check
					TailInfo firsttail = firstnode.gettail(Node.flip_dir(first.dir));
					if (!rtail.id.equals(firsttail.id))
					{
						throw new IOException("Rtail->tail->tail != Rtail");
					}

					// merge string
					String mstr = rtnode.str();
					if (mergedir.equals("r"))
					{
						mstr = Node.rc(mstr);
						rtnode.revreads();
					}

					TailInfo cur = new TailInfo(first);

					int mergelen = 0;

					Node curnode = nodes.get(cur.id);

					int merlen = mstr.length() - K + 1;
					int covlen = merlen;

					double covsum = rtnode.cov() * merlen;

					int shift = merlen;

					String lastid = cur.id;
					String lastdir = cur.dir;

					while (!cur.id.equals(ftail.id))
					{
						curnode = nodes.get(cur.id);

						if (VERBOSE) { System.err.println(curnode.toNodeMsg(true)); }

						// curnode can be deleted
						curnode.setCustom(DONE, "2");
						mergelen++;

						String bstr = curnode.str();
						if (cur.dir.equals("r")) 
						{ 
							bstr = Node.rc(bstr);
							curnode.revreads();
						}
					
						mstr = Node.str_concat(mstr, bstr, K);

						merlen = bstr.length() - K + 1;
						covsum += curnode.cov() * merlen;
						covlen += merlen;

						rtnode.addreads(curnode, shift);
						shift += merlen;

						lastid = cur.id;
						lastdir = cur.dir;

						cur = curnode.gettail(lastdir);
					}

					if (VERBOSE) { System.err.println(ftnode.toNodeMsg(true)); }
					if (VERBOSE) { System.err.println("=="); }

					// If we made it all the way to the ftail, 
					// see if we should do the final merge
					if ((domerge == 2) && 
							(cur.id.equals(ftail.id)) && 
							(mergelen == (chainlen-1)))
					{
						mergelen++;
						rtnode.setCustom(DONE, "2");

						String bstr = ftnode.str();
						if (cur.dir.equals("r"))
						{
							bstr = Node.rc(bstr);
							ftnode.revreads();
						}

						mstr = Node.str_concat(mstr, bstr, K);

						merlen = bstr.length() - K + 1;
						covsum += ftnode.cov() * merlen;
						covlen += merlen;

						rtnode.addreads(ftnode, shift);

						// we want the same orientation for ftail as before
						if (cur.dir.equals("r")) { mstr = Node.rc(mstr); }
						ftnode.setstr(mstr);

						// Copy reads over
						ftnode.setR5(rtnode);
						if (cur.dir.equals("r")) { ftnode.revreads(); }

						ftnode.setCoverage((float) covsum / (float) covlen);

						// Update ftail's new neigbors to be rtail's old neighbors
						// Update the rtail neighbors to point at ftail
						// Update the can compress flags
						// Update threads

						// Clear the old links from ftnode in the direction of the chain
						ftnode.clearEdges(ftail.dir + "f");
						ftnode.clearEdges(ftail.dir + "r");

						// Now move the links from rtnode to ftnode
						for (String adj : Node.dirs)
						{
							String origdir = Node.flip_dir(rtail.dir) + adj;
							String newdir  = ftail.dir + adj;

							//System.err.println("Shifting " + rtail.id + " " + origdir);

							List<String> vl = rtnode.getEdges(origdir);

							if (vl != null)
							{
								for (String v : vl)
								{
									if (v.equals(rtail.id))
									{
										// Cycle on rtail
										if (VERBOSE) { System.err.println("Fixing rtail cycle"); }

										String cycled = ftail.dir;

										if (rtail.dir.equals(adj)) { cycled += Node.flip_dir(ftail.dir); }
										else                       { cycled += ftail.dir; }

										ftnode.addEdge(cycled, ftail.id);
									}
									else
									{
										ftnode.addEdge(newdir, v);

										Node vnode = nodes.get(v);
										vnode.replacelink(rtail.id, Node.flip_link(origdir),
												ftail.id, Node.flip_link(newdir));
									}
								}
							}
						}

						// Now move the can compresflag from rtnode into ftnode
						ftnode.setCanCompress(ftail.dir, rtnode.canCompress(Node.flip_dir(rtail.dir)));

						// Break cycles
						for (String dir : Node.dirs)
						{
							TailInfo next = ftnode.gettail(dir);

							if ((next != null) && next.id.equals(ftail.id))
							{
								if (VERBOSE) { System.err.println("Breaking tail " + ftail.id); }
								ftnode.setCanCompress("f", false);
								ftnode.setCanCompress("r", false);
							}
						}

						// Confirm there are no threads in $ftnode in $fdir
						List<String> threads = ftnode.getThreads();
						if (threads != null)
						{
							ftnode.clearThreads();

							for (String thread : threads)
							{
								String [] vals = thread.split(":"); // t, link, read

								if (!vals[0].substring(0,1).equals(ftail.dir))
								{
									ftnode.addThread(vals[0], vals[1], vals[2]);  
								}
							}
						}

						// Now copy over rtnodes threads in !$rdir
						threads = rtnode.getThreads();
						if (threads != null)
						{
							for (String thread : threads)
							{
								String [] vals = thread.split(":"); // t, link, read
								if (!vals[0].substring(0,1).equals(rtail.dir))
								{
									String et = ftail.dir + vals[0].substring(1);
									ftnode.addThread(et, vals[1], vals[2]);
								}

							}
						}

						if (VERBOSE) { System.err.println(ftnode.toNodeMsg(true)); }
						if (VERBOSE) { System.err.println("==]"); }
					}
					else
					{
						if (mergelen < chainlen)
						{
							System.err.println("Hit an unexpected cycle mergelen: " + mergelen + " chainlen: " + chainlen + " in " + rtnode.getNodeId() + " " + ftnode.getNodeId() + " mertag:" + mertag.toString());
							System.err.println(rtnode.toNodeMsg(true));
							System.err.println(ftnode.toNodeMsg(true));
							throw new IOException("Hit an unexpected cycle mergelen: " + mergelen + " chainlen: " + chainlen + " in " + rtnode.getNodeId() + " " + ftnode.getNodeId() + " mertag:" + mertag.toString());
						}

						if (mergedir.equals("r")) 
						{ 
							mstr = Node.rc(mstr);
							rtnode.revreads();
						}

						rtnode.setstr(mstr);

						rtnode.setCoverage((float) covsum / (float) covlen);

						String mergeftaildir = lastdir;
						if (!lastdir.equals(mergedir)) { mergeftaildir = Node.flip_dir(mergeftaildir); }

						// update rtail->first with rtail->ftail link
						rtnode.replacelink(first.id, mergedir + first.dir, 
								ftail.id, mergeftaildir + cur.dir);

						ftnode.replacelink(lastid, Node.flip_link(lastdir+cur.dir),
								rtail.id,Node.flip_link(mergeftaildir + cur.dir));

						if (curnode.getThreads() != null)
						{
							//throw new IOException("ERROR: curnode has threads " + curnode.toNodeMsg(true));
							curnode.cleanThreads();
						}

						if (VERBOSE) { System.err.println(rtnode.toNodeMsg(true)); }
						if (VERBOSE) { System.err.println("==]"); }
					}

					saved  += mergelen;
					cchains++;
				}
			}

			for(String nodeid : nodes.keySet())
			{
				Node node = nodes.get(nodeid);
				if (node.hasCustom(DONE) && node.getCustom(DONE).get(0).equals("1"))
				{
					output.collect(new Text(node.getNodeId()), new Text(node.toNodeMsg()));
				}
			}

			reporter.incrCounter("Contrail", "chains",        chains);
			reporter.incrCounter("Contrail", "cchains",       cchains);
			reporter.incrCounter("Contrail", "totalchainlen", totallen);
			reporter.incrCounter("Contrail", "saved",         saved);
						}
	}




	public RunningJob run(String inputPath, String outputPath) throws Exception
	{ 
		sLogger.info("Tool name: QuickMerge");
		sLogger.info(" - input: "  + inputPath);
		sLogger.info(" - output: " + outputPath);

		JobConf conf = new JobConf(Stats.class);
		conf.setJobName("QuickMerge " + inputPath + " " + ContrailConfig.K);

		ContrailConfig.initializeConfiguration(conf);

		FileInputFormat.addInputPath(conf, new Path(inputPath));
		FileOutputFormat.setOutputPath(conf, new Path(outputPath));

		conf.setInputFormat(TextInputFormat.class);
		conf.setOutputFormat(TextOutputFormat.class);

		conf.setMapOutputKeyClass(Text.class);
		conf.setMapOutputValueClass(Text.class);

		conf.setOutputKeyClass(Text.class);
		conf.setOutputValueClass(Text.class);

		conf.setMapperClass(QuickMergeMapper.class);
		conf.setReducerClass(QuickMergeReducer.class);

		//delete the output directory if it exists already
		FileSystem.get(conf).delete(new Path(outputPath), true);

		return JobClient.runJob(conf);
	}


	public int run(String[] args) throws Exception 
	{

		Option kmer = new Option("k","k",true,"k. The length of each kmer to use.");
		Option input = new Option("input","input",true,"The directory containing the input (i.e the output of BuildGraph.)");
		Option output = new Option("output","output",true,"The directory where the output should be written to.");

		Options options = new Options();
		options.addOption(kmer);
		options.addOption(input);
		options.addOption(output);

		CommandLineParser parser = new GnuParser();

		CommandLine line = parser.parse( options, args );


		if (!line.hasOption("input") || !line.hasOption("output") || !line.hasOption("k")){
			System.out.println("ERROR: Missing required arguments");
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp( "QuickMerge", options );
		}

		String inputPath  = line.getOptionValue("input");
		String outputPath = line.getOptionValue("output");
		ContrailConfig.K = Integer.parseInt(line.getOptionValue("k"));

		ContrailConfig.hadoopBasePath = "foo";
		ContrailConfig.hadoopReadPath = "foo";

		run(inputPath, outputPath);
		//TODO: do we need to parse the other options?
		return 0;
	}

	public static void main(String[] args) throws Exception 
	{
		int res = ToolRunner.run(new Configuration(), new QuickMerge(), args);
		System.exit(res);
	}
}

