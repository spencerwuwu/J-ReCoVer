// https://searchcode.com/api/result/73390624/

/*
    PreCorrect.java
    2012 a  ReadStackCorrector, developed by Chien-Chih Chen (rocky@iis.sinica.edu.tw), 
    released under Apache License 2.0 (http://www.apache.org/licenses/LICENSE-2.0) 
    at: https://github.com/ice91/ReadStackCorrector
*/

package Corrector;

import java.io.RandomAccessFile ;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Set;
import java.util.List;
import java.util.Map;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.filecache.DistributedCache;
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


public class PreCorrect extends Configured implements Tool
{
	private static final Logger sLogger = Logger.getLogger(PreCorrect.class);

	public static class PreCorrectMapper extends MapReduceBase
    implements Mapper<LongWritable, Text, Text, Text>
	{
		//public static int TRIM5 = 0;
		//public static int TRIM3 = 0;
        private static int IDX = 0;
        private static Path[] localFiles;
        private static HashSet<String> HKmer_List = new HashSet<String>();

		public void configure(JobConf job)
		{
            IDX = Integer.parseInt(job.get("IDX"));
            /*try {
                localFiles = DistributedCache.getLocalCacheFiles(job);
            } catch (IOException ioe) {
                System.err.println("Caught exception while getting cached files: " + ioe.toString());
            }
            try {
            	String str;
            	String str_r;
            	File folder = new File(localFiles[0].toString());
            	for (File fileEntry : folder.listFiles()) {
            		//\\
            		//\\read distributed cache file : load data from file to id_seq   
                    RandomAccessFile f = new RandomAccessFile(fileEntry.getAbsolutePath(), "r");
                    FileChannel ch = f.getChannel();
                    String first_line = "BBBEBBBBBBBB	";
                    int readlen = first_line.length(); // to determine the size of one record in the file
                    byte[] buffer = new byte[readlen];
                    long buff_start = 0; //store buffer start position
                    int buff_pos = 0; // fetch data from this buffer position
                    
                    // determine the buffer size
                    long read_content = Math.min(Integer.MAX_VALUE, ch.size() - buff_start); //0x8FFFFFF = 128MB
                    MappedByteBuffer mb = ch.map(FileChannel.MapMode.READ_ONLY, buff_start, read_content);
                    String kmer;
                    while ( mb.hasRemaining( ) && mb.position()+readlen <= read_content ) {
                    	mb.get(buffer);
                    	kmer = new String(buffer);
                    	str = Node.dna2str(kmer.trim());
	            		str_r = Node.rc(str);
	            		HKmer_List.add(str);
	            		HKmer_List.add(str_r);
                    }
                    ch.close();
                    f.close();
            		//\\
                }
            } catch (IOException ioe){
            	System.err.println("Caught exception while reading cached files: " + ioe.toString());
            }*/
		}

		public void map(LongWritable lineid, Text nodetxt,
				        OutputCollector<Text, Text> output, Reporter reporter)
		                throws IOException
		{	
            Node node = new Node();
			node.fromNodeMsg(nodetxt.toString());
            
            //slide the split K-mer windows for each read in both strands
            int end = node.len() - IDX -1;
            for (int i = 0; i < end; i++)
            {
                String window_tmp = node.str().substring(i, i+(IDX/2)) + node.str().substring(i+(IDX/2+1), i+(IDX+1));
                String window_tmp_r = Node.rc(window_tmp);
                // H-kmer filter
                /*if (HKmer_List.contains(window_tmp)) {
                	reporter.incrCounter("Brush", "hkmer", 1);
                	continue;
                }*/
                //\\
                if (window_tmp.compareTo(window_tmp_r) < 0) {
                    String prefix_half_tmp = window_tmp.substring(0, IDX/2);
                    String suffix_half_tmp = window_tmp.substring(IDX/2);
                    String prefix_half = Node.str2dna(prefix_half_tmp);
                    String suffix_half = Node.str2dna(suffix_half_tmp);
                    int f_pos = i + (IDX/2);
                    if ( !window_tmp.matches("A*") && !window_tmp.matches("T*") ){
                         output.collect(new Text(prefix_half),
                                   new Text(node.getNodeId() + "\t" + "f" + "\t" + f_pos + "\t" + node.str().charAt(f_pos) + "\t" + node.QV().charAt(f_pos) + "\t" + suffix_half + "\t" + node.str().length()));
                    }
                } else if (window_tmp_r.compareTo(window_tmp) < 0) {
                    String prefix_half_tmp_r = window_tmp_r.substring(0, IDX/2);
                    String suffix_half_tmp_r = window_tmp_r.substring(IDX/2);
                    String prefix_half_r = Node.str2dna(prefix_half_tmp_r);
                    String suffix_half_r = Node.str2dna(suffix_half_tmp_r);
                    int r_pos = end - i + (IDX/2);
                    //String Qscore_reverse = new StringBuffer(node.Qscore_1()).reverse().toString();
                    String QV_reverse = new StringBuffer(node.QV()).reverse().toString();
                    if ( !window_tmp_r.matches("A*") && !window_tmp_r.matches("T*") ){
                        //try { 
                    	output.collect(new Text(prefix_half_r),
                                   new Text(node.getNodeId() + "\t" + "r" + "\t" + r_pos + "\t" + Node.rc(node.str()).charAt(r_pos) + "\t" + node.QV().charAt(r_pos) + "\t" + suffix_half_r + "\t" + node.str().length()));
                        //} catch (Exception e) {
                        //	throw new IOException("node_len: " + node.str().length() + " r_pos: " + r_pos + " qv_len: " + node.Qscore_1().length() + " r_pos: " + r_pos  );
                        //}
                    }
                }
                
                //debug
                //output.collect(new Text(HKmer_List.get(0)),new Text(HKmer_List.size()+""));
               
                //\\
                
                /*int f_pos = i + (IDX/2);
                
                if ( !window_tmp.matches("A*") && !window_tmp.matches("T*") && !window_tmp.equals(window_r_tmp)) {
                    output.collect(new Text(window),
                                   new Text(node.getNodeId() + "\t" + "f" + "\t" + f_pos + "\t" + node.str().charAt(f_pos) + "\t" + node.Qscore_1().charAt(f_pos)));
                }*/
                
                /*String window_r_tmp = Node.rc(node.str().substring(node.len()-(IDX+1)-i, node.len()-(IDX/2+1)-i) + node.str().substring(node.len()-(IDX/2)-i, node.len()-i));
                String window_r = Node.str2dna(window_r_tmp);
                int r_pos = node.len()-(IDX/2+1)-i;
                if (!window_tmp.matches("A*") && !window_tmp.matches("T*") && !window_tmp.equals(window_r_tmp)) {
                    output.collect(new Text(window_r),
                                   new Text(node.getNodeId() + "\t" + "r" + "\t" + r_pos + "\t" + Node.rc(node.str().charAt(r_pos) + "") + "\t" + node.Qscore_1().charAt(r_pos)));
                }*/
            }
            
		}
	}

	public static class PreCorrectReducer extends MapReduceBase
	implements Reducer<Text, Text, Text, Text>
	{
		private static int K = 0;
        private static long HighKmer = 0;
        private static long IDX = 0;

		public void configure(JobConf job) {
			K = Integer.parseInt(job.get("K"));
            HighKmer = Long.parseLong(job.get("UP_KMER"));
            IDX = Integer.parseInt(job.get("IDX"));
		}

        public class ReadInfo
		{
			public String id;
            public boolean dir;
			public short pos;
            public byte base;
            public byte qv;
            public short len;

			public ReadInfo(String id1, String dir1, short pos1, String base1, String qv1, short len1) throws IOException
			{
				id = id1;
                if (dir1.equals("f")) {
                    dir = true;
                } else {
                    dir = false;
                }
                pos = pos1;
                base = base1.getBytes()[0];
                //qv = qv1.getBytes()[0];
                if (qv1.equals("A")) {
                    qv = 0;
                } else if (qv1.equals("T")) {
                    qv = 10;
                } else if (qv1.equals("C")) {
                    qv = 20;
                } else if (qv1.equals("G")) {
                    qv = 30;
                }
                len = len1;
			}

            public String toString()
			{
				return id + "!" + dir + "|" + pos + "|" + base + "|" + qv + "[" + ((int)qv-33) +"]";
			}
		}
        
        class ReadComparator implements Comparator {
            public int compare(Object element1, Object element2) {
                ReadInfo obj1 = (ReadInfo) element1;
                ReadInfo obj2 = (ReadInfo) element2;
                if ((int) ( obj1.pos - obj2.pos ) > 0) {
                    return -1;
                } else if ((int) ( obj1.pos - obj2.pos ) < 0) {
                    return 1;
                } else {
                    if ( obj1.id.compareTo(obj2.id) < 0) {
                        return -1;
                    } else {
                        return 1;
                    }
                }
            }
        }
        
		public void reduce(Text prefix, Iterator<Text> iter,
						   OutputCollector<Text, Text> output, Reporter reporter)
						   throws IOException
		{
            List<String> corrects_list = new ArrayList<String>();
			//List<ReadInfo> readlist = new ArrayList<ReadInfo>();
            List<ReadInfo> readlist;
            Map<String, List<ReadInfo>> ReadStack_list = new HashMap<String, List<ReadInfo>>();
            
            int prefix_sum = 0;
            int belong_read = 0;
            int kmer_count = 0;
            //List<String> ReadID_list = new ArrayList<String>();
            
            
            while(iter.hasNext())
			{
				String msg = iter.next().toString();
				String [] vals = msg.split("\t");
                ReadInfo read_item = new ReadInfo(vals[0],vals[1],Short.parseShort(vals[2]), vals[3], vals[4], Short.parseShort(vals[6]));
                //\\
                String window_tmp = Node.dna2str(prefix.toString()) + Node.dna2str(vals[5]);
                String window = Node.str2dna(window_tmp);
                if (ReadStack_list.get(window) != null) {
                    readlist = ReadStack_list.get(window);
                    readlist.add(read_item);
                    ReadStack_list.put(window, readlist);
                } else {
                    readlist = new ArrayList<ReadInfo>();
                    readlist.add(read_item);
                    ReadStack_list.put(window, readlist);
                }   
			}
            
            for(String RS_idx : ReadStack_list.keySet())
            {// for each readstack 
            readlist = ReadStack_list.get(RS_idx);   
            //\\
            if (readlist.size() <= 5) {
                continue;
            }
            
            //\\ DEBUG
            //Collections.sort(readlist, new ReadComparator());
            //int left_len = readlist.get(0).pos;
            //\\
            ReadInfo[] readarray = readlist.toArray(new ReadInfo[readlist.size()]);
            readlist.clear();
            
            //\\\\\\\
            //\\DEBUG
            /*output.collect(new Text("MSG"), new Text( "[" + Node.dna2str(RS_idx) + "]" + RS_idx));
            for(int i=0; i < readarray.length; i++){
                //\\ DEBUG
                String start_pos="";
                for(int j=0; j < (left_len - (readarray[i].pos)); j++) {
                    start_pos = start_pos + " ";
                }
                output.collect(new Text("MSG"), new Text(start_pos + " " + new String((char)readarray[i].base+"")+ " " + readarray[i].dir + " " + readarray[i].id + " " + readarray[i].pos + " " + new String((char)readarray[i].base+"")));
            }*/
            //\\\\\\\ debug
            //\\\
            
            //\\ 0:A 1:T 2:C 3:G 4:Count
            int[] base_array = new int[5];
            for(int i=0; i<5; i++) {
                base_array[i] = 0;
            }
            boolean lose_A = true;
            boolean lose_T = true;
            boolean lose_C = true;
            boolean lose_G = true;
            for(int i=0; i < readarray.length; i++) {
                ReadInfo readitem = readarray[i];
                base_array[4] = base_array[4] + 1;
                //int quality_value = ((int)readitem.qv-33);
                int quality_value = ((int)readitem.qv);
                char base_char = (char)readitem.base;
                if (quality_value < 0 ) {
                    quality_value = 0;
                } else if (quality_value > 40) {
                    quality_value = 40;
                }
                if (base_char == 'A'){
                    base_array[0] = base_array[0] + quality_value;
                    if (quality_value >= 20) {
                        lose_A = false;
                    }
                } else if (base_char == 'T') {
                    base_array[1] = base_array[1] + quality_value;
                    if (quality_value >= 20) {
                        lose_T = false;
                    }
                } else if (base_char == 'C') {
                    base_array[2] = base_array[2] + quality_value;
                    if (quality_value >= 20) {
                        lose_C = false;
                    }
                } else if (base_char == 'G') {
                    base_array[3] = base_array[3] + quality_value;
                    if (quality_value >= 20) {
                        lose_G = false;
                    }
                }
            }
            
            char correct_base = 'N';
            int majority = 60;
            int reads_threshold = 6;
            float winner_sum = 0;
            if (base_array[0] > base_array[1] && base_array[0] > base_array[2] && base_array[0] > base_array[3] && base_array[0] >= majority && base_array[4] >= reads_threshold) {
                correct_base = 'A';
                winner_sum = base_array[0];
            } else if (base_array[1] > base_array[0] && base_array[1] > base_array[2] && base_array[1] > base_array[3] && base_array[1] >= majority && base_array[4] >= reads_threshold) {
                correct_base = 'T';
                winner_sum = base_array[1];
            } else if (base_array[2] > base_array[0] && base_array[2] > base_array[1] && base_array[2] > base_array[3] && base_array[2] >= majority && base_array[4] >= reads_threshold) {
                correct_base = 'C';
                winner_sum = base_array[2];
            } else if (base_array[3] > base_array[0] && base_array[3] > base_array[1] && base_array[3] > base_array[2] && base_array[3] >= majority && base_array[4] >= reads_threshold) {
                correct_base = 'G';
                winner_sum = base_array[3];
            }
            
            //output.collect(new Text("DEBUG"), new Text( "[" + correct_base + "]" + base_array[0] + "|" + base_array[1] + "|" + base_array[2] + "|" + base_array[3]));
            if (correct_base != 'N') {
                reporter.incrCounter("Brush", "base_notN", 1);
                boolean fix = true;
                for(int i=0; i < readarray.length; i++) {
                    ReadInfo readitem = readarray[i];
                    if ((char)readitem.base != correct_base) {
                        //\\
                        if ((char)readitem.base == 'A' && ((float)base_array[0]/(float)winner_sum > 0.25f /*|| !lose_A*/ )){
                            fix = false;
                            //continue;
                        }
                        if ((char)readitem.base == 'T' && ((float)base_array[1]/(float)winner_sum > 0.25f /*|| !lose_T*/ )){
                            fix = false;
                            //continue;
                        }
                        if ((char)readitem.base == 'C' && ((float)base_array[2]/(float)winner_sum > 0.25f /*|| !lose_C*/ )){
                            fix = false;
                            //continue;
                        }
                        if ((char)readitem.base == 'G' && ((float)base_array[3]/(float)winner_sum > 0.25f /*|| !lose_G*/ )){
                            fix = false;
                            //continue;
                        }
                        //\\
                        if (readitem.dir && fix) {
                            String correct_msg =readitem.id + "," + readitem.pos + "," + correct_base;
                            //if (!corrects_list.contains(correct_msg)){
                                corrects_list.add(correct_msg);
                            //}
                            //\\
                            //output.collect(new Text("COR"), new Text(correct_msg));
                            //\\
                            reporter.incrCounter("Brush", "fix_char", 1);
                        } 
                        if (!readitem.dir && fix) {
                            int pos = readitem.len-1-readitem.pos;
                            String correct_msg = readitem.id + "," + pos + "," + Node.rc(correct_base+"");
                            //if (!corrects_list.contains(correct_msg)){
                                corrects_list.add(correct_msg);
                                //\\
                                //output.collect(new Text("COR"), new Text(correct_msg));
                                //\\
                                reporter.incrCounter("Brush", "fix_char", 1);
                            //} 
                        }
                    }
                }
            }
            } //for each read stack
            // replace close() function
            // create fake node to pass message
            Node node = new Node("MSG");
            node.setstr_raw("X");
            node.setCoverage(1);
            // remove redundant correct msg
            corrects_list = new ArrayList(new HashSet(corrects_list));
            
            Map<String, List<String>> out_list = new HashMap<String, List<String>>();
            for(int i=0; i < corrects_list.size(); i++) {
                String[] vals = corrects_list.get(i).split(",");
                String id = vals[0];
                String msg = vals[1] + "," + vals[2];
                if (out_list.containsKey(id)){
                    List<String> tmp_corrects = out_list.get(id);
                    tmp_corrects.add(msg);
                    out_list.put(id, tmp_corrects);
                } else {
                    List<String> tmp_corrects = new ArrayList<String>();
                    tmp_corrects.add(msg);
                    out_list.put(id, tmp_corrects);
                }
                //mOutput.collect(new Text(vals[0]), new Text(vals[1] + "," + vals[2]));
            }

            for(String read_id : out_list.keySet())
            {
                String msgs="";
                List<String> correct_msg = out_list.get(read_id);
                // to many correct msg may cause by repeat
                msgs = correct_msg.get(0);
                if ( correct_msg.size() > 1) {
                    for (int i=1; i < correct_msg.size(); i++) {
                        msgs = msgs + "!" + correct_msg.get(i);
                    }
                } 
                output.collect(new Text(read_id), new Text(msgs));
                //node.addCorrections(read_id, msgs);
            }
            /*if (corrects_list.size() > 0 ) {
                output.collect(new Text(node.getNodeId()), new Text(node.toNodeMsg()));
            }*/
		}
	}



	public RunningJob run(String inputPath, String outputPath, int idx, String hkmerlist) throws Exception
	{
		sLogger.info("Tool name: PreCorrect");
		sLogger.info(" - input: "  + inputPath);
		sLogger.info(" - output: " + outputPath);

		JobConf conf = new JobConf(PreCorrect.class);
		conf.setJobName("PreCorrect " + inputPath + " " + Config.K);
        conf.setLong("IDX", idx);
        //\\
        DistributedCache.addCacheFile(new URI(hkmerlist), conf);
        //\\
		
        Config.initializeConfiguration(conf);

		FileInputFormat.addInputPath(conf, new Path(inputPath));
		FileOutputFormat.setOutputPath(conf, new Path(outputPath));

		conf.setInputFormat(TextInputFormat.class);
		conf.setOutputFormat(TextOutputFormat.class);

		conf.setMapOutputKeyClass(Text.class);
		conf.setMapOutputValueClass(Text.class);

		conf.setOutputKeyClass(Text.class);
		conf.setOutputValueClass(Text.class);

		conf.setMapperClass(PreCorrectMapper.class);
		conf.setReducerClass(PreCorrectReducer.class);

		//delete the output directory if it exists already
		FileSystem.get(conf).delete(new Path(outputPath), true);

		return JobClient.runJob(conf);
	}

	public int run(String[] args) throws Exception
	{
		String inputPath  = "";
		String outputPath = "";
		Config.K = 21;

		long starttime = System.currentTimeMillis();

		run(inputPath, outputPath, 0,"XX");

		long endtime = System.currentTimeMillis();

		float diff = (float) (((float) (endtime - starttime)) / 1000.0);

		System.out.println("Runtime: " + diff + " s");

		return 0;
	}

	public static void main(String[] args) throws Exception
	{
		int res = ToolRunner.run(new Configuration(), new PreCorrect(), args);
		System.exit(res);
	}
}


