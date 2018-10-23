// https://searchcode.com/api/result/73390706/

/*
    KmerFrequencyOfReads.java
    2012 a  ReadStackCorrector, developed by Chien-Chih Chen (rocky@iis.sinica.edu.tw), 
    released under Apache License 2.0 (http://www.apache.org/licenses/LICENSE-2.0) 
    at: https://github.com/ice91/ReadStackCorrector
*/

package Corrector;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.List;
import java.util.Map;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.IntWritable;
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


public class KmerFrequencyOfReads extends Configured implements Tool
{
	private static final Logger sLogger = Logger.getLogger(KmerFrequencyOfReads.class);

	public static class KmerFrequencyOfReadsMapper extends MapReduceBase
    implements Mapper<LongWritable, Text, Text, Text>
	{
		public static int K = 0;
		public static int TRIM5 = 0;
		public static int TRIM3 = 0;

		public void configure(JobConf job)
		{
			K = 24;//Integer.parseInt(job.get("K"));
		}

		public void map(LongWritable lineid, Text nodetxt,
				        OutputCollector<Text, Text> output, Reporter reporter)
		                throws IOException
		{
            Node node = new Node();
			node.fromNodeMsg(nodetxt.toString());
            if (!node.hasCustom("n")){
                reporter.incrCounter("Brush", "nodes", 1);
                //slide the K-mer windows for each read in both strands
                int end = node.len() - K;
                for (int i = 0; i < end; i++)
                {
                    String window_tmp = node.str().substring(i,   i+K);
                    //String window_r_tmp = Node.rc(node.str().substring(node.len() - K - i, node.len() - i));
                    String window_tmp_r = Node.rc(window_tmp);
                    if (window_tmp.compareTo(window_tmp_r) < 0) {
                        String window = Node.str2dna(window_tmp);
                        output.collect(new Text(window), new Text((int)node.cov() + "|" + node.getNodeId()));
                    } else {
                        String window_r = Node.str2dna(window_tmp_r);
                        output.collect(new Text(window_r), new Text((int)node.cov() + "|" + node.getNodeId()));
                    }
                    //String window_r = Node.str2dna(window_r_tmp);
                    /*String window = Node.str2dna(window_tmp);
                    String window_r = Node.str2dna(window_r_tmp);
                    output.collect(new Text(window), new Text((int)node.cov() + "|" + node.getNodeId()));
                    output.collect(new Text(window_r), new Text((int)node.cov() + "|" + node.getNodeId()));
                    reporter.incrCounter("Brush", "Allkmer", (int)node.cov());*/
                }
            }
		}
	}

	public static class KmerFrequencyOfReadsReducer extends MapReduceBase
	implements Reducer<Text, Text, Text, Text>
	{
		private static int K = 0;
        public static long HighKmer = 0;
        //private static int OVALSIZE = 0;
        private static int All_Kmer = 0;

		public void configure(JobConf job) {
			K = 24;//Integer.parseInt(job.get("K"));
            HighKmer = Long.parseLong(job.get("UP_KMER"));
		}

		public void reduce(Text prefix, Iterator<Text> iter,
						   OutputCollector<Text, Text> output, Reporter reporter)
						   throws IOException
		{
            int sum =0;
            //int read_count = 0;
            List<String> ReadID_list = new ArrayList<String>();
            //List<String> ReadID_list;
            //Map<String, List<String>> idx_ReadID_list = new HashMap<String, List<String>>();
            while(iter.hasNext())
			{
                String msg = iter.next().toString();
				String [] vals = msg.split("\\|");
                //\\
                if (ReadID_list.contains(vals[1])){
                    // do nothing
                } else {
                    //read_count = read_count + 1;
                    sum += Integer.parseInt(vals[0]);
                    ReadID_list.add(vals[1]);
                    
                }
                if (ReadID_list.size() > 2 ) {
                    return;
                }
            }
            
            for(int i=0; i < ReadID_list.size(); i++) {
                output.collect(new Text(ReadID_list.get(i)), new Text(sum+""));
            }
            
		}
	}



	public RunningJob run(String inputPath, String outputPath) throws Exception
	{
		sLogger.info("Tool name: KmerFrequencyOfReads");
		sLogger.info(" - input: "  + inputPath);
		sLogger.info(" - output: " + outputPath);

		JobConf conf = new JobConf(KmerFrequencyOfReads.class);
		conf.setJobName("KmerFrequencyOfReads " + inputPath + " " + Config.K);

		Config.initializeConfiguration(conf);

		FileInputFormat.addInputPath(conf, new Path(inputPath));
		FileOutputFormat.setOutputPath(conf, new Path(outputPath));

		conf.setInputFormat(TextInputFormat.class);
		conf.setOutputFormat(TextOutputFormat.class);

		conf.setMapOutputKeyClass(Text.class);
		//conf.setMapOutputValueClass(IntWritable.class);
        conf.setMapOutputValueClass(Text.class);

		conf.setOutputKeyClass(Text.class);
		conf.setOutputValueClass(Text.class);

		conf.setMapperClass(KmerFrequencyOfReads.KmerFrequencyOfReadsMapper.class);
		conf.setReducerClass(KmerFrequencyOfReads.KmerFrequencyOfReadsReducer.class);

		//delete the output directory if it exists already
		FileSystem.get(conf).delete(new Path(outputPath), true);

		return JobClient.runJob(conf);
	}

	public int run(String[] args) throws Exception
	{
		String inputPath  = "/cygdrive/contrail-bio/data/Ec10k.sim.sfa";
		String outputPath = "/cygdrive/contrail-bio/";
		Config.K = 21;

		long starttime = System.currentTimeMillis();

		run(inputPath, outputPath);

		long endtime = System.currentTimeMillis();

		float diff = (float) (((float) (endtime - starttime)) / 1000.0);

		System.out.println("Runtime: " + diff + " s");

		return 0;
	}

	public static void main(String[] args) throws Exception
	{
		int res = ToolRunner.run(new Configuration(), new KmerFrequencyOfReads(), args);
		System.exit(res);
	}
}

