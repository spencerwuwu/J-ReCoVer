// https://searchcode.com/api/result/64481674/

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
//import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

//import org.apache.accumulo.core.data.Range;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;

public class StepSortReducer extends Reducer<dynamic_fs_stats.CompositeKey,Text,Text,Text> {
	private Text tKey = new Text();
	private Text tValue = new Text();
	private HashMap<String, String> map = new HashMap<String, String>();
	
	public void reduce(dynamic_fs_stats.CompositeKey key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
		map.clear();
		
		long q1_bytes = 0;
		long q2_bytes = 0;
		long q3_bytes = 0;
		//long file_count = 0;
		while (values.iterator().hasNext()) {
			String[] spl = values.iterator().next().toString().split(";");
			String path = spl[0];
			long size = Long.parseLong(spl[1]);
			String accessDate = spl[2];
			
			
			//System.out.println(key.ugi+"-"+key.accessDate+"-"+String.valueOf(key.size)+"="+path+":"+accessDate+":"+String.valueOf(size));
			
			if(size < 0) {
				map.put(path, "");
			} else {
				if(map.containsKey(path)) {
					//Q3: Temporary file
					q3_bytes += size;
				} else {
					q1_bytes += size;
					try {
						String queryDate = "2010-04-21";
						
						SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
						Calendar c = Calendar.getInstance();
						c.setTime(sdf.parse(queryDate));
						c.set(Calendar.DAY_OF_YEAR, c.get(Calendar.DAY_OF_YEAR) - c.get(Calendar.DAY_OF_WEEK));
						Date startDate = c.getTime();
						c.add(Calendar.DATE, 7);
						Date endDate = c.getTime();
						
						c.setTime(sdf.parse(accessDate));
						Date accessDateD = c.getTime();
						if(accessDateD.after(startDate) && accessDateD.before(endDate)) {
							q2_bytes += size;
						}
					} catch (ParseException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}				
				}
			}
		}
		
		tKey.set("q1:"+key.ugi);
		tValue.set(String.valueOf(q1_bytes));
		context.write(tKey, tValue);
		System.out.println(tKey.toString() + " "+tValue.toString());
		
		tKey.set("q2:"+key.ugi);
		tValue.set(String.valueOf(q2_bytes));
		context.write(tKey, tValue);
		System.out.println(tKey.toString() + " "+tValue.toString());
		
		tKey.set("q3:"+key.ugi);
		tValue.set(String.valueOf(q3_bytes));
		context.write(tKey, tValue);
		System.out.println(tKey.toString() + " "+tValue.toString());
	}
}
