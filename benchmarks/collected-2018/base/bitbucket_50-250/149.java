// https://searchcode.com/api/result/64481660/

import java.io.IOException;
//import java.text.SimpleDateFormat;
//import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Map;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;

public class Step3Reducer extends Reducer<Text,Text,Text,Text> {
	//Get date of first day of week
	/*String getWeekTime(String dateEntry) {
		try {
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
			Calendar c = Calendar.getInstance();
			c.setTime(sdf.parse(dateEntry));
			c.set(Calendar.DAY_OF_YEAR, c.get(Calendar.DAY_OF_YEAR) - c.get(Calendar.DAY_OF_WEEK));
			dateEntry = sdf.format(c.getTime());
		}
		catch(Exception e) {}
		
		return dateEntry;
	}*/
	
	private Text tKey = new Text();
	private Text tValue = new Text();
	private void writeOutput(Context c, String path, String ugi, String size,  String timeC,  String timeD,  String timeR) {
		try {	
			tKey.set(path);
			tValue.set(ugi + " "  + size + " " + timeC + " " + timeD + " " + timeR);
			c.write(tKey, tValue);
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
	}
	
	//Readers in the duration across which average
	class DurReaders{
		public DurReaders() {
			readers = new LinkedList<String>();
			readerCount = 1;
			bytesCount = 0L;
		}
		
		public Integer readerCount;
		public Long bytesCount;
		public LinkedList<String> readers;
	}
	
	Map<String, DurReaders> durInfo = new HashMap<String, DurReaders>();
	
	public void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
		
		//Read path empty => writer.
		if(key.toString().equalsIgnoreCase(dynamic_fs_stats.unknownPath)) {
			for (Text val : values) {
				RowValue tempRv = new RowValue(val.toString(), false);
				if(!tempRv.sizeW.equals(dynamic_fs_stats.unknownBlockSize)) {
					writeOutput(context, tempRv.path, tempRv.ugi, tempRv.sizeW, tempRv.timeC, tempRv.timeD, dynamic_fs_stats.unknownTime);
				}
			}	
			return;
		}
		
		long MAXREADER = 400; //Due to memory limitations
		
		String pathWriterUgi = dynamic_fs_stats.unknownUgi;
		String writerPath = dynamic_fs_stats.unknownPath;
		String timeC = dynamic_fs_stats.unknownTime;
		String timeD = dynamic_fs_stats.unknownTime;
		
		long totalBytesWritten = 0;
		String readDate;
		
		for (Text val : values) {
			RowValue tempRv = new RowValue(val.toString(), false);
			//System.out.println("RV:" + tempRv.toString());
			
			if (!tempRv.ugiR.equals(dynamic_fs_stats.unknownUgi)) {
				//readDate = getWeekTime(tempRv.timeR);
				readDate = tempRv.timeR;
				
				//Increment reader count. Create entry if first.
				if(!durInfo.containsKey(key)) {
					durInfo.put(readDate, new DurReaders());
				} else {
					durInfo.get(readDate).readerCount = durInfo.get(readDate).readerCount+1;
				}
				
				//Add reader to list
				if(durInfo.get(readDate).readerCount < MAXREADER) {
					durInfo.get(readDate).readers.add(tempRv.ugiR);
				}
				
				//Increment bytes read
				if (!tempRv.sizeR.equals(dynamic_fs_stats.unknownBlockSize)) {
					durInfo.get(readDate).bytesCount = durInfo.get(readDate).bytesCount+Long.parseLong(tempRv.sizeR);
				}
			}
			
			if (!tempRv.ugi.equals(dynamic_fs_stats.unknownUgi)) {
				pathWriterUgi = tempRv.ugi;
			}
			
			if (!tempRv.path.equals(dynamic_fs_stats.unknownPath)) {
				writerPath = tempRv.path;
			}
			
			if (!tempRv.timeC.equals(dynamic_fs_stats.unknownTime)) {
				timeC = tempRv.timeC;
			}
			
			if (!tempRv.timeD.equals(dynamic_fs_stats.unknownTime)) {
				timeD = tempRv.timeD;
			}
			
			if (!tempRv.sizeW.equals(dynamic_fs_stats.unknownBlockSize)) {
				totalBytesWritten = Long.parseLong(tempRv.sizeW);
			}
		}

		if( totalBytesWritten != 0 && !pathWriterUgi.equals(dynamic_fs_stats.unknownUgi)) {	
			writeOutput(context, writerPath, pathWriterUgi, String.valueOf(totalBytesWritten), timeC, timeD, dynamic_fs_stats.unknownTime);
		}
		
		String bytesRead = "0";
		LinkedList<String> readers;
		ListIterator<String> itr;
		DurReaders dur;
		String reader;
		for ( String durTime : durInfo.keySet()){
			dur = durInfo.get(durTime);
			if (dur.readerCount != 0) {
				bytesRead = String.valueOf(dur.bytesCount/dur.readerCount);
				readers = dur.readers;
				itr = readers.listIterator(); //Reader ugi
				while(itr.hasNext())
			    {
					reader = itr.next().toString();
			    	if (!bytesRead.equals(dynamic_fs_stats.unknownBlockSize)) {
			    		writeOutput(context, key.toString(), reader, bytesRead, dynamic_fs_stats.unknownTime, dynamic_fs_stats.unknownTime, durTime);
			    	}
			    }
			}
		}
		
		for (DurReaders v : durInfo.values()) {
		    v.readers.clear();
		}
		durInfo.clear();
	}
}
