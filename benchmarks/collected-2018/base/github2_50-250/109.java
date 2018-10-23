// https://searchcode.com/api/result/111931701/

package us.yuxin.demo.hadoop.zero.userlogin;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;

public class ULUserReducer2 extends
		Reducer<Text, IntWritable, IntWritable, ULDistribution> {
	
	private HashMap<Integer, ULDistribution> monReducer;

	
	
	@Override
	protected void reduce(Text key, Iterable<IntWritable> values, Context context)
			throws IOException, InterruptedException {
		
		ULDistribution uld = new ULDistribution();
		for (IntWritable month : values) {
			uld.add(month.get());
		}
		
		//first.set(uld.firstLogin());
		int first = uld.firstLogin();
		
		ULDistribution base = monReducer.get(first);
		if (base == null) {
			monReducer.put(first, uld);
		} else {
			base.add(uld);
		}
	}



	@Override
	protected void cleanup(Context context)
			throws IOException, InterruptedException {
		for (Map.Entry<Integer, ULDistribution> entry : monReducer.entrySet()) {
			context.write(new IntWritable(entry.getKey()), entry.getValue());
		}
		super.cleanup(context);
	}

	@Override
	protected void setup(Context context)
			throws IOException, InterruptedException {
		super.setup(context);
		monReducer = new HashMap<Integer, ULDistribution>();
	}
}

