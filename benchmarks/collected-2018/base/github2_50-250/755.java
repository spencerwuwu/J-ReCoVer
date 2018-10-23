// https://searchcode.com/api/result/67830881/

package preData;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import lib.Point;
import lib.ToolJob;
import lib.VectorWritable;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.filecache.DistributedCache;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.SequenceFileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.SequenceFileOutputFormat;
import org.apache.log4j.Logger;

public class CreatePoint extends ToolJob {

	private static Logger logger = Logger.getLogger(CreatePoint.class);

	public static class MyMapper extends Mapper<Text, Text, Text, Text> {
		@Override
		public void map(Text nid, Text line, Context context) {
			String[] tmp1 = line.toString().split(",");
			if (tmp1.length != 2) {
				return;
			}
			String uid = tmp1[0];
			try {
				context.write(new Text(uid), nid);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public static class MyReducer extends Reducer<Text, Text, IntWritable, Point> {
		private Map<String,Integer> nidMap = new HashMap<String,Integer>();

		@Override
		public void reduce(Text uid, Iterable<Text> nidList, Context context) {
			HashMap<Integer,Double> data = new HashMap<Integer,Double>();
			
			for (Text nid : nidList) {
				if (!nidMap.containsKey(nid.toString())) {
					continue;
				}
				Integer position = nidMap.get(nid.toString());
				if (data.containsKey(position)) {
					Double clicktimes = data.get(position) + 1;
					data.put(position, clicktimes);
				}
				else {
					data.put(position, 1.0);
				}
			}
			
			VectorWritable vector = new VectorWritable(nidMap.size());
			vector.setData(data);
			vector.setColumnNum(nidMap.size());
			
			Point point = new Point();
			point.setData(vector);
			try {
				point.setId(Integer.parseInt(uid.toString()));
			}catch(Exception e) {
				return;
			}
			try {
				context.write(new IntWritable(point.getId()), point);
			} catch (IOException e) {

				e.printStackTrace();
			} catch (InterruptedException e) {

				e.printStackTrace();
			}
		}

		@Override
		protected void setup(Context context) {
			Configuration conf = context.getConfiguration();
//			String nidFile = conf.get("nidFile");
			try {
				URI[] uriList = DistributedCache.getCacheFiles(conf);
				for (URI uri : uriList) {
					logger.debug(uri);
					System.out.print("uri:------"+uri);
				}
				Path[] paths = DistributedCache.getLocalCacheFiles(conf);
				for( Path path :  paths) {
					System.out.println("path:===="+path);
				}
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			FileReader fr;
			Set<String> nidSet = new HashSet<String>();
			
			try {
				Path[] paths = DistributedCache.getLocalCacheFiles(conf);
				File file = new File(paths[0].toString());
				if (file.exists()) {
					System.out.println("exists file:---"+file);
				}
				
				fr = new FileReader(paths[0].toString());
				BufferedReader br = new BufferedReader(fr);
				String line = "";
				while ((line = br.readLine()) != null) {
					String tmp1[] = line.split("\t");
					if (tmp1.length == 0)
						continue;
					String nid = tmp1[0];
					nidSet.add(nid);
					System.out.println("nid:    "+nid);
					
				}
				br.close();
				fr.close();
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}// FileReader
			catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			int i=0;
			for (String nid : nidSet) {
				nidMap.put(nid, i);
				i++;
			}

		}
	}	

	/* (non-Javadoc)
	 * @see org.apache.hadoop.util.Tool#run(java.lang.String[])
	 * 
	 * SequenceFileclicknid->uid,time
	 */
	@Override
	public int run(String[] args) throws Exception {
		if (args.length != 2) {
			System.out.println("wrong number of args");
			return 0;
		}
		String input = args[0];
		String output = args[1];
//		String nidFile = args[2];
		
		

		Configuration conf = getConf();
		
//		DistributedCache.addCacheFile(new URI(nidFile), conf);
//		nidFile = nidFile.substring(nidFile.lastIndexOf("/")+1,nidFile.length());
//		conf.set("nidFile", nidFile);
		
		Job job = new Job(conf, "createVector");
		job.setJarByClass(CreatePoint.class);
		job.setMapperClass(MyMapper.class);
		job.setMapOutputKeyClass(Text.class);
		job.setMapOutputValueClass(Text.class);
		job.setReducerClass(MyReducer.class);
		job.setOutputKeyClass(IntWritable.class);
		job.setOutputValueClass(Point.class);
		job.setInputFormatClass(SequenceFileInputFormat.class);
		job.setOutputFormatClass(SequenceFileOutputFormat.class);
		
		FileInputFormat.addInputPath(job, new Path(input));
		FileOutputFormat.setOutputPath(job, new Path(output));
		
		
		if (job.waitForCompletion(true))
			return 1;
		else
			return 0;
	}

}

