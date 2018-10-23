// https://searchcode.com/api/result/110935996/

package joins.multiway;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.*;
import org.apache.hadoop.mapred.lib.MultipleTextOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.apache.hadoop.conf.*;

@SuppressWarnings("deprecation")
public class CardinalityCounter extends Configured implements Tool {

	public RunningJob job;
	
	public static class CounterMap extends MapReduceBase 
		implements Mapper<Text, Text, Text, Text>
	{
		String inputFile = "";

		@Override
		public void configure(JobConf conf) {
			super.configure(conf);
			
			inputFile = conf.get("map.input.file");
		}

		@Override
		public void map(Text key, Text value,
				OutputCollector<Text, Text> output, Reporter reporter)
				throws IOException {
			output.collect(key, new Text(inputFile));
		}
		
	}
	
	public static class CounterReduce extends MapReduceBase 
		implements Reducer<Text, Text, Text, IntWritable>{

//		String[] tables;
//		@Override
//		public void configure(JobConf conf) {
//			super.configure(conf);
//			tables = conf.get("mapred.input.dir").split(";");
//		}

		@Override
		public void reduce(Text key, Iterator<Text> values,
				OutputCollector<Text, IntWritable> output, Reporter reporter)
				throws IOException {

			HashMap<String, Integer> hm = new HashMap<String, Integer>();
			while(values.hasNext())
			{
				Text value = values.next();
				if(hm.containsKey(value.toString()))
				{
					Integer currentVal = hm.get(value.toString());
					hm.put(value.toString(), currentVal+1);
				}
				else
				{
					hm.put(value.toString(), 1);
				}
			}
			String[] tables = hm.keySet().toArray(new String[0]);
			for(int i=0;i<tables.length - 1;i++)
			{
				for(int j=i+1;j<tables.length;j++)
				{
					String counter = tables[i].substring(tables[i].lastIndexOf("/")+1)+
									"+"+tables[j].substring(tables[j].lastIndexOf("/")+1);
					reporter.incrCounter("OutputCardinality",counter, 
							hm.get(tables[i])*hm.get(tables[j]));
				}
				output.collect(new Text(tables[i]+";"+key.toString()), new IntWritable(hm.get(tables[i])));
			}
			output.collect(new Text(tables[tables.length-1]+";"+key.toString()), 
					new IntWritable(hm.get(tables[tables.length-1])));
		}
	}
	
	public static class CounterMultipleTextOutputFormat
		extends MultipleTextOutputFormat<Text, IntWritable>
	{
		@Override
		protected Text generateActualKey(Text key, IntWritable value) {
			return new Text(key.toString().split(";")[1]);
		}

		@Override
		protected String generateFileNameForKeyValue(Text key,
				IntWritable value, String name) {
			String inputFile = key.toString().split(";")[0]; 
			return inputFile.substring(inputFile.lastIndexOf("/")+1);
		}
		
	}
	
	@Override
	public int run(String[] args) throws Exception {			
		
		JobConf conf = new JobConf(CardinalityCounter.class);
		for(String s : args)
		{
			System.out.println(s);
		}
		conf.setJobName(args[0]);
		//Path[] plist = new Path[args.length-2];
		for(int i=1;i<args.length-1;i++)
		{
			FileInputFormat.addInputPath(conf, new Path(args[i])); 
		}
		
		conf.setInputFormat(KeyValueTextInputFormat.class);
		
		conf.setMapperClass(CounterMap.class);
		conf.setMapOutputKeyClass(Text.class);
		conf.setMapOutputValueClass(Text.class);
		
		
		conf.setReducerClass(CounterReduce.class);
		conf.setOutputKeyClass(Text.class);
		conf.setOutputValueClass(IntWritable.class);
		
		
		FileOutputFormat.setOutputPath(conf, new Path(args[args.length - 1]));
		conf.setOutputFormat(CounterMultipleTextOutputFormat.class);
		
		JobClient jc = new JobClient(conf);
		
		ClusterStatus cluster = jc.getClusterStatus();
		conf.setNumReduceTasks(cluster.getMaxReduceTasks()-2);
		
		job = JobClient.runJob(conf);
		
		return 0;
	}

	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		int exitCode = ToolRunner.run(new CardinalityCounter(), args);
	    System.exit(exitCode);

	}

}

