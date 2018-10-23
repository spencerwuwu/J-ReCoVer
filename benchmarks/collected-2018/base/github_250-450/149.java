// https://searchcode.com/api/result/73390673/

/*
    PCorrection.java
    2012 a  ReadStackCorrector, developed by Chien-Chih Chen (rocky@iis.sinica.edu.tw), 
    released under Apache License 2.0 (http://www.apache.org/licenses/LICENSE-2.0) 
    at: https://github.com/ice91/ReadStackCorrector
*/
package Corrector;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

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



public class PCorrection extends Configured implements Tool
{
	private static final Logger sLogger = Logger.getLogger(PCorrection.class);


	// PopBubblesMapper
	///////////////////////////////////////////////////////////////////////////

	public static class PCorrectionMapper extends MapReduceBase
    implements Mapper<LongWritable, Text, Text, Text>
	{
		private static int K = 0;
        private static Node node = new Node();
        
		public void configure(JobConf job)
		{
			K = Integer.parseInt(job.get("K"));
		}

		
        public void map(LongWritable lineid, Text nodetxt,
				OutputCollector<Text, Text> output, Reporter reporter)
		throws IOException
		{
            String vals[] = nodetxt.toString().split("\t");
            if (vals[1].equals(Node.NODEMSG)){
               node.fromNodeMsg(nodetxt.toString());
               output.collect(new Text(node.getNodeId()), new Text(node.toNodeMsg()));
            } else {
               output.collect(new Text(vals[0]), new Text(Node.CORRECTMSG + "\t" + vals[1]));
            }
			reporter.incrCounter("Brush", "nodes", 1);
            /*node.fromNodeMsg(nodetxt.toString());
            if (node.str_raw().equals("X")) {
                List<String> corrections = node.getCorrections();
                if (corrections != null)
                {
                    for(String correction : corrections)
                    {
                        String [] vals = correction.split("\\|");
                        String id    = vals[0];
                        String correct_msg   = vals[1];

                        output.collect(new Text(id),
                                       new Text(Node.CORRECTMSG + "\t" + correct_msg));

                    }

                    node.clearCorrections();
                }   
            } else {
                output.collect(new Text(node.getNodeId()), new Text(node.toNodeMsg()));
                reporter.incrCounter("Brush", "nodes", 1);
            }*/
		}
	}

	// PCorrectionReducer
	///////////////////////////////////////////////////////////////////////////

	public static class PCorrectionReducer extends MapReduceBase
	implements Reducer<Text, Text, Text, Text>
	{
		private static int K = 0;

		public void configure(JobConf job) {
			K = Integer.parseInt(job.get("K"));
		}

        public class Fix
		{
		    //public String node_id;
		    public List<String> pos;

		    public Fix(String[] vals, int offset) throws IOException
		    {
		    	if (!vals[offset].equals(Node.CORRECTMSG))
		    	{
		    		throw new IOException("Unknown msg");
		    	}
                pos = new ArrayList<String>();
                String pos_lists  = vals[offset+1];
                String [] val2s = pos_lists.split("!");
                for(int i=0; i < val2s.length; i++) {
                    pos.add(val2s[i]);
                }     
		    }
		}
        
         public class Correct
        {
            public char chr;
            public int pos;
            public Correct(int pos1, char chr1) throws IOException
            {
                pos = pos1;
                chr = chr1;
            }
        }

		public void reduce(Text nodeid, Iterator<Text> iter,
				OutputCollector<Text, Text> output, Reporter reporter)
		throws IOException
		{
			Node node = new Node(nodeid.toString());

			int sawnode = 0;

			boolean killnode = false;
			float extracov = 0;
            //List<Fix> fixs = new ArrayList<Fix>();
            List<Correct> corrects = new ArrayList<Correct>();

			while(iter.hasNext())
			{
				String msg = iter.next().toString();

				//System.err.println(nodeid.toString() + "\t" + msg);

				String [] vals = msg.split("\t");

				if (vals[0].equals(Node.NODEMSG))
				{
					node.parseNodeMsg(vals, 0);
					sawnode++;
				}
                else if (vals[0].equals(Node.CORRECTMSG))
				{
                    String pos_lists  = vals[1];
                    String [] val2s = pos_lists.split("!");
                    for(int i=0; i < val2s.length; i++) {
                        String[] vals3 = val2s[i].split(",");
                        corrects.add(new Correct(Integer.parseInt(vals3[0]), vals3[1].charAt(0)));
                    }     
				}
				else
				{
					throw new IOException("Unknown msgtype: " + msg);
				}
			}

			if (sawnode != 1)
			{
                throw new IOException("ERROR: Didn't see exactly 1 nodemsg (" + sawnode + ") for " + nodeid.toString());
			}
            
            boolean failed_reads = false;
            Correct[] c_array = corrects.toArray(new Correct[corrects.size()]);
            corrects.clear();
            if (c_array.length > 0)
			{
                //\\ 0:A 1:T 2:C 3:G 4:Sum 
                boolean exclusive[] = new boolean[node.len()];
                int[][] array = new int[node.len()][5];
                for(int i=0; i < node.len(); i++) {
                    exclusive[i] = true;
                    for(int j=0; j < 5; j++) {
                        array[i][j] = 0;
                    }
                }
                //\\\
                for(int i=0; i < c_array.length; i++) {
                    //String [] vals = fix_msg.split(",");
                    int pos = c_array[i].pos;
                    char fix_chr = c_array[i].chr;
                    array[pos][4] = array[pos][4] + 1;
                    if (fix_chr == 'A') {
                        array[pos][0] = array[pos][0] + 1;
                    } else if (fix_chr == 'T') {
                        array[pos][1] = array[pos][1] + 1;
                    } else if (fix_chr == 'C') {
                        array[pos][2] = array[pos][2] + 1;
                    } else if (fix_chr == 'G') {
                        array[pos][3] = array[pos][3] + 1;
                    }      
                }
                //\\\
                
                // fix str content
                float majority = 0.99f;
                String fix_str = ""; //node.str().substring(0, pos) + fix_char + node.str().substring(pos+1); 
                String fix_qv = "";
                //\\determine exclusive
                for(int i=0; i < array.length; i++) {
                    int left_corrects = 0;
                    int right_corrects = 0;
                    // add left_side correction
                    for(int j=i-1; j>=0 && j>=i-12; j--) {
                        left_corrects = left_corrects + array[j][4];
                    }
                    // add right_sum correction
                    for(int j=i+1; j < array.length && j<=i+12; j++) {
                        right_corrects = right_corrects + array[j][4];
                    }
                    if (right_corrects == 0 && left_corrects ==0) {
                        exclusive[i] = true;
                    }
                }
                //\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\
                for(int i=0; i < array.length; i++){
                    if (array[i][4] > 0 && array[i][0]==array[i][4] && exclusive[i] == true ) {
                        fix_str = fix_str + "A";
                        fix_qv = fix_qv + "A"; // A == 0
                        reporter.incrCounter("Brush", "fix_char", 1);
                    } else if (array[i][4] > 0 && array[i][1]==array[i][4] && exclusive[i] == true) {
                        fix_str = fix_str + "T";
                        fix_qv = fix_qv + "A"; // A == 0
                        reporter.incrCounter("Brush", "fix_char", 1);
                    } else if (array[i][4] > 0 && array[i][2]==array[i][4] && exclusive[i] == true) {
                        fix_str = fix_str + "C";
                        fix_qv = fix_qv + "A"; // A == 0
                        reporter.incrCounter("Brush", "fix_char", 1);
                    } else if (array[i][4] > 0 && array[i][3]==array[i][4] && exclusive[i] == true) {
                        fix_str = fix_str + "G";
                        fix_qv = fix_qv + "A"; // A == 0
                        reporter.incrCounter("Brush", "fix_char", 1);
                    } else {
                        fix_str = fix_str + node.str().charAt(i);
                        fix_qv = fix_qv + node.QV().charAt(i);
                    }
                }
                node.setstr(fix_str);
                node.setQV(fix_qv);
            }
            output.collect(nodeid, new Text(node.toNodeMsg()));
		}
	}




	// Run Tool
	///////////////////////////////////////////////////////////////////////////

	public RunningJob run(String inputPath, String outputPath) throws Exception
	{
		sLogger.info("Tool name: PCorrection");
		sLogger.info(" - input: "  + inputPath);
		sLogger.info(" - output: " + outputPath);

		JobConf conf = new JobConf(PCorrection.class);
		conf.setJobName("PCorrection " + inputPath + " " + Config.K);

		Config.initializeConfiguration(conf);

		FileInputFormat.addInputPaths(conf, inputPath);
		FileOutputFormat.setOutputPath(conf, new Path(outputPath));

		conf.setInputFormat(TextInputFormat.class);
		conf.setOutputFormat(TextOutputFormat.class);

		conf.setMapOutputKeyClass(Text.class);
		conf.setMapOutputValueClass(Text.class);
        //conf.setBoolean("mapred.output.compress", true);

		conf.setOutputKeyClass(Text.class);
		conf.setOutputValueClass(Text.class);

		conf.setMapperClass(PCorrectionMapper.class);
		conf.setReducerClass(PCorrectionReducer.class);

		//delete the output directory if it exists already
		FileSystem.get(conf).delete(new Path(outputPath), true);

		return JobClient.runJob(conf);
	}


	// Parse Arguments and run
	///////////////////////////////////////////////////////////////////////////

	public int run(String[] args) throws Exception
	{
		String inputPath  = "";
		String outputPath = "";

		Config.K = 21;

		run(inputPath, outputPath);
		return 0;
	}


	// Main
	///////////////////////////////////////////////////////////////////////////

	public static void main(String[] args) throws Exception
	{
		int res = ToolRunner.run(new Configuration(), new PCorrection(), args);
		System.exit(res);
	}
}




