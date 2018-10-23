// https://searchcode.com/api/result/75864646/

package com.sohu.adrd.video.search;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import javax.sound.midi.MidiDevice.Info;

import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.Reducer.Context;
import org.apache.pig.data.Tuple;

import com.sohu.adrd.data.sessionlog.consume.ZebraBaseMapper;
import com.sohu.adrd.data.sessionlog.thrift.operation.SearchOperation;
import com.sohu.adrd.data.summary.MRProcessor;



public class VideoSearch extends MRProcessor {
	
	public static class ZebraMapper extends ZebraBaseMapper<Text, LongWritable> {

		public static final String CG_SEARCH = "search";
		
		
		public void setup(Context context) throws IOException,
				InterruptedException {
			super.setup(context);
			projection = context.getConfiguration().get("mapreduce.lib.table.input.projection");
		}
		
		@SuppressWarnings( { "unchecked", "deprecation" })
		public void map(BytesWritable key, Tuple value, Context context)
				throws IOException, InterruptedException {
			
			List<SearchOperation> searchList = new ArrayList<SearchOperation>();
			List<String> albums = new ArrayList<String>();
			
			BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream("album0904"), "UTF-8"));
			String str;
			while ((str = br.readLine()) != null) {
				albums.add(str.trim());
			}
			br.close();
		
			decode(key, value);
		
			searchList = (List<SearchOperation>) list.get(CG_SEARCH);
			
			if(searchList!=null) {
				for(SearchOperation info: searchList) {
					for(String album:albums) {
						String queryword = info.getKeywords();
						if(queryword.contains(album)) {
							context.write(new Text(album+"\t"+info.getDomain()), new LongWritable(1));
						}
					}
				}
			}		
		}
	}


	public static class HReduce extends
			Reducer<Text, LongWritable, Text, LongWritable> {

		public void reduce(Text key, Iterable<LongWritable> values,
				Context context) throws IOException, InterruptedException {
			long sum = 0;
			for (LongWritable value : values) {
				sum += value.get();
			}
			context.write(key, new LongWritable(sum));
		}
	}

	@Override
	protected void configJob(Job job) {
		job.setMapperClass(ZebraMapper.class);
		job.setReducerClass(HReduce.class);
		job.setCombinerClass(HReduce.class);
		job.setMapOutputKeyClass(Text.class);
		job.setMapOutputValueClass(LongWritable.class);
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(LongWritable.class);
	}

}

