// https://searchcode.com/api/result/124699299/

/**
 * 
 */
package org.jaggu.hadoop.invidx;

import java.io.IOException;
import java.util.StringTokenizer;

import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.lib.input.FileSplit;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

/**
 * @author Jaganadh G
 *
 */
public class InvertedIndex extends Configured implements Tool {
	
	//Mapper BEGIN

	public static class InvertedIndexMapper extends Mapper<LongWritable, Text, Text, Text>{
		public final static Text strToken = new Text();
		public final static Text strFileName = new Text();
		
		public void map(LongWritable key, Text value, Context context){
							
			FileSplit files = (FileSplit) context.getInputSplit();
			String strHDFSFileName = files.getPath().getName();
			strFileName.set(strHDFSFileName);
			
			String strFileContent = value.toString().replaceAll("\\p{Punct}+", " ");
			StringTokenizer tokenizer = new StringTokenizer(strFileContent.toLowerCase());
			while(tokenizer.hasMoreTokens()){
				strToken.set(tokenizer.nextToken());
				try {
					context.write(strToken, strFileName);
				} catch (IOException exception) {
					exception.printStackTrace();
				} catch (InterruptedException exception) {
					exception.printStackTrace();
				}
			}
						
		}
	}
	//Mapper END
	
	//Reducer BEGIN
	public static class InvertedIndexReducer extends Reducer<Text, Text, Text, Text>{
		    public void reduce(Text key, Iterable<Text> values, Context context){
        	boolean initialString = true;
        	StringBuilder strIndexItem = new StringBuilder();
        	
        	while(values.iterator().hasNext()){
        		if(!initialString){
        			strIndexItem.append(",");
        		}
        		initialString = false;
        		strIndexItem.append(values.iterator().next().toString());
        	}
        	try {
				context.write(key, new Text(strIndexItem.toString()));
			} catch (IOException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
        	       	
        }
	}
	//Reducer END
	@Override
	public int run(String[] arg0) throws Exception {
		Path strInputPath = new Path("inverted");
		
		String inpath = "bigramtest";
		Path strOutputPath = new Path("invertedIndex");
		
		Job invertedIndexJob = new Job(getConf());
		//invertedIndexJob
		
		invertedIndexJob.setJarByClass(InvertedIndex.class);
		invertedIndexJob.setJobName("inverted_index");
		
		invertedIndexJob.setOutputKeyClass(Text.class);
		invertedIndexJob.setOutputValueClass(Text.class);
		
		invertedIndexJob.setMapperClass(InvertedIndexMapper.class);
		invertedIndexJob.setCombinerClass(InvertedIndexReducer.class);
		invertedIndexJob.setReducerClass(InvertedIndexReducer.class);
		
		FileInputFormat.setInputPaths(invertedIndexJob, strInputPath);
		FileOutputFormat.setOutputPath(invertedIndexJob, strOutputPath);
		
		boolean success = invertedIndexJob.waitForCompletion(true);
		return success ? 0 : 1;

	}
	
	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		int ret = ToolRunner.run(new InvertedIndex(), args);
		System.exit(ret);


	}


}

