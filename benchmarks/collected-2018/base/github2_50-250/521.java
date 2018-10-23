// https://searchcode.com/api/result/75864617/

package com.sohu.adrd.data.summary;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.Mapper.Context;
import org.apache.pig.data.Tuple;

import com.sohu.adrd.data.sessionlog.consume.ZebraBaseMapper;
import com.sohu.adrd.data.sessionlog.thrift.operation.CountinfoOperation;
import com.sohu.adrd.data.sessionlog.thrift.operation.PVOperation;
import com.sohu.adrd.data.sessionlog.thrift.operation.SearchOperation;



public class Summary extends MRProcessor {
	
	public static class ZebraMapper extends ZebraBaseMapper<Text, LongWritable> {

		public static final String CG_USER = "user";
		public static final String CG_ADDISPLAY = "addisplay";
		public static final String CG_ADCLICK = "adclick";
		public static final String CG_NEWSDISPLAY = "newsdisplay";
		public static final String CG_NEWSCLICK = "newsclick";
		public static final String CG_SEARCH = "search";
		public static final String CG_PV = "pv";
		public static final String CG_HBDISPLAY = "hbdisplay";
		public static final String CG_HBCLICK = "hbclick";
		public static final String CG_ARRIVE = "arrive";
		

		public void setup(Context context) throws IOException,
				InterruptedException {
			super.setup(context);
			projection = context.getConfiguration().get(
					"mapreduce.lib.table.input.projection",
					"user,addisplay,adclick");
		}
		
		@SuppressWarnings( { "unchecked", "deprecation" })
		public void map(BytesWritable key, Tuple value, Context context)
				throws IOException, InterruptedException {
			List<CountinfoOperation> advList = new ArrayList<CountinfoOperation>();
			List<CountinfoOperation> adclkList = new ArrayList<CountinfoOperation>();
			List<CountinfoOperation> newsvList = new ArrayList<CountinfoOperation>();
			List<CountinfoOperation> newsclkList = new ArrayList<CountinfoOperation>();
			List<CountinfoOperation> hbvList = new ArrayList<CountinfoOperation>();
			List<CountinfoOperation> hbcList = new ArrayList<CountinfoOperation>();
			List<CountinfoOperation> arrList = new ArrayList<CountinfoOperation>();
			
			List<PVOperation> pvList = new ArrayList<PVOperation>();
			List<SearchOperation> searchList = new ArrayList<SearchOperation>();


			decode(key, value);
			
			advList = (List<CountinfoOperation>) list.get(CG_ADDISPLAY);
			adclkList = (List<CountinfoOperation>) list.get(CG_ADCLICK);
			newsvList = (List<CountinfoOperation>) list.get(CG_NEWSDISPLAY);
			newsclkList = (List<CountinfoOperation>) list.get(CG_NEWSCLICK);
			searchList = (List<SearchOperation>) list.get(CG_SEARCH);
			pvList = (List<PVOperation>) list.get(CG_PV);
			hbvList = (List<CountinfoOperation>) list.get(CG_HBDISPLAY);
			hbcList = (List<CountinfoOperation>) list.get(CG_HBCLICK);
			arrList = (List<CountinfoOperation>) list.get(CG_ARRIVE);
			
			
			context.write(new Text("User"), new LongWritable(1));
			
			if(advList!=null) {
				for(CountinfoOperation info: advList) {
					String type = info.getAdType();
					type = "null".equalsIgnoreCase(type) ? "string_null" : type;
					context.write(new Text("AdDisplay\t"+type), new LongWritable(1));
					context.write(new Text("AdDisplay\tAll"), new LongWritable(1));
//					for(int i = 0; i < 64; i++) {
//						if((info.getStatusCode() ^ (1L << i)) != 0L) {
//							context.write(new Text("AdDisplay\t"+type+" error pos: " + i), new LongWritable(1));
//						}
//					}
				}
			}
			
			if(adclkList!=null) {
				for(CountinfoOperation info: adclkList) {
					String type = info.getAdType();
					type = "null".equalsIgnoreCase(type) ? "string_null" : type;
					context.write(new Text("AdClick\t"+type), new LongWritable(1));
					context.write(new Text("AdClick\tAll"), new LongWritable(1));
//					for(int i = 0; i < 64; i++) {
//						if((info.getStatusCode() ^ (1L << i)) != 0L) {
//							context.write(new Text("AdDisplay\t"+type+" error pos: " + i), new LongWritable(1));
//						}
//					}
				}
			}
			
			if(newsvList!=null) {
				for(CountinfoOperation info: newsvList) {
					String type = info.getExt();
					type = "null".equalsIgnoreCase(type) ? "string_null" : type;
					context.write(new Text("NewsDisplay\t"+type), new LongWritable(1));
					context.write(new Text("NewsDisplay\tAll"), new LongWritable(1));
//					for(int i = 0; i < 64; i++) {
//						if((info.getStatusCode() ^ (1L << i)) != 0L) {
//							context.write(new Text("AdDisplay\t"+type+" error pos: " + i), new LongWritable(1));
//						}
//					}
				}
			}
			
			if(newsclkList!=null) {
				for(CountinfoOperation info: newsclkList) {
					String type = info.getExt();
					type = "null".equalsIgnoreCase(type) ? "string_null" : type;
					context.write(new Text("NewsClick\t"+type), new LongWritable(1));
					context.write(new Text("NewsClick\tAll"), new LongWritable(1));
//					for(int i = 0; i < 64; i++) {
//						if((info.getStatusCode() ^ (1L << i)) != 0L) {
//							context.write(new Text("AdDisplay\t"+type+" error pos: " + i), new LongWritable(1));
//						}
//					}
				}
			}
			
			if(searchList!=null) {
				for(SearchOperation info: searchList) {
					context.write(new Text("Search\t"), new LongWritable(1));
				}
			}
			
			if(pvList!=null) {
				for(PVOperation info: pvList) {
					context.write(new Text("Pv\t"), new LongWritable(1));
				}
			}
			
			if(hbvList != null) {
				for(CountinfoOperation info:hbvList) {
					context.write(new Text("HBDisplay\t"), new LongWritable(1));
				}
			}
			
			if(hbcList != null) {
				for(CountinfoOperation info:hbcList) {
					context.write(new Text("HBClick\t"), new LongWritable(1));
				}
			}
			
			if(arrList != null) {
				for(CountinfoOperation info: arrList) {
					context.write(new Text("Arrive\t"), new LongWritable(1));
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

