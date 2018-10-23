// https://searchcode.com/api/result/75864640/

package com.sohu.adrd.video.search;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.pig.data.Tuple;

import com.sohu.adrd.data.sessionlog.consume.ZebraBaseMapper;
import com.sohu.adrd.data.sessionlog.thrift.operation.CountinfoOperation;
import com.sohu.adrd.data.sessionlog.thrift.operation.PVOperation;
import com.sohu.adrd.data.sessionlog.thrift.operation.SearchOperation;
import com.sohu.adrd.data.summary.MRProcessor;

public class Userid2SUV extends MRProcessor {
	public static class ZebraMapper extends ZebraBaseMapper<Text, Text> {
		
		public static final String CG_USER = "user";
		public static final String CG_PV = "pv";
		public static final String CG_SEARCH = "search";
		public static final String CG_ADDISPLAY = "addisplay";
		public static final String CG_ADCLICK = "adclick";
		public static final String CG_NEWSDISPLAY = "newsdisplay";
		public static final String CG_NEWSCLICK = "newsclick";
		public static final String CG_HBDISPLAY = "hbdisplay";
		public static final String CG_HBCLICK = "hbclick";
		public static final String CG_ARRIVE = "arrive";
		public static final String CG_REACH = "reach";
		public static final String CG_ACTION = "action";
		public static final String CG_ERR = "err";
		public static final String CG_EXCHANGE = "exchange";
		public static final String CG_CM = "cookiemapping";
		
		
		public void setup(Context context) throws IOException,
				InterruptedException {
			super.setup(context);
			projection = context.getConfiguration().get("mapreduce.lib.table.input.projection");
		}
		
		@SuppressWarnings( { "unchecked", "deprecation" })
		public void map(BytesWritable key, Tuple value, Context context)
				throws IOException, InterruptedException {
			
			decode(key, value);
			
			String userid = (String) list.get(CG_USER);
			if(userid == null) return;
			
			List<SearchOperation> searchList = new ArrayList<SearchOperation>();
			searchList = (List<SearchOperation>) list.get(CG_SEARCH);
			
			if(searchList!=null) {
				for(SearchOperation info: searchList) {
					String suv = info.getSuv();
					if(suv == null) return;
					context.write(new Text(userid), new Text(suv));
					return;
				}
			}
			//---------------------------------------------
			List<PVOperation> pvList = new ArrayList<PVOperation>();
			pvList = (List<PVOperation>) list.get(CG_SEARCH);
			
			if(pvList!=null) {
				for(PVOperation info: pvList) {
					String suv = info.getSuv();
					if(suv == null) return;
					context.write(new Text(userid), new Text(suv));
					return;
				}
			}
			//---------------------------------------------
			List<CountinfoOperation> adclickList = new ArrayList<CountinfoOperation>();
			adclickList = (List<CountinfoOperation>) list.get(CG_ADCLICK);
			
			if(adclickList!=null) {
				for(CountinfoOperation info: adclickList) {
					String suv = info.getSuv();
					if(suv == null) return;
					context.write(new Text(userid), new Text(suv));
					return;
				}
			}
			//---------------------------------------------
			List<CountinfoOperation> addisplayList = new ArrayList<CountinfoOperation>();
			addisplayList = (List<CountinfoOperation>) list.get(CG_ADDISPLAY);
			
			if(addisplayList!=null) {
				for(CountinfoOperation info: addisplayList) {
					String suv = info.getSuv();
					if(suv == null) return;
					context.write(new Text(userid), new Text(suv));
					return;
				}
			}
			//---------------------------------------------
			List<CountinfoOperation> newsclickList = new ArrayList<CountinfoOperation>();
			newsclickList = (List<CountinfoOperation>) list.get(CG_NEWSCLICK);
			
			if(newsclickList!=null) {
				for(CountinfoOperation info: newsclickList) {
					String suv = info.getSuv();
					if(suv == null) return;
					context.write(new Text(userid), new Text(suv));
					return;
				}
			}
			//---------------------------------------------
			
			List<CountinfoOperation> newsdisplay = new ArrayList<CountinfoOperation>();
			newsdisplay = (List<CountinfoOperation>) list.get(CG_NEWSDISPLAY);
			
			if(newsdisplay!=null) {
				for(CountinfoOperation info: newsdisplay) {
					String suv = info.getSuv();
					if(suv == null) return;
					context.write(new Text(userid), new Text(suv));
					return;
				}
			}
			//---------------------------------------------
			
		}
	}


	public static class HReduce extends
			Reducer<Text, Text, Text, LongWritable> {

		public void reduce(Text key, Iterable<Text> values,
				Context context) throws IOException, InterruptedException {
			
		}
	}

	@Override
	protected void configJob(Job job) {
		job.setMapperClass(ZebraMapper.class);
		job.setReducerClass(HReduce.class);
		job.setCombinerClass(HReduce.class);
		job.setMapOutputKeyClass(Text.class);
		job.setMapOutputValueClass(Text.class);
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(LongWritable.class);
	}

}

