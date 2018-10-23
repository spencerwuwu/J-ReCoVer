// https://searchcode.com/api/result/69377656/

package mon4h.framework.dashboard.mapreduce.predowansample;

import com.ctrip.framework.hbase.client.util.HBaseClientUtil;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.KeyValue;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.hadoop.hbase.mapreduce.TableReducer;
import org.apache.hadoop.hbase.util.Bytes;

import java.io.IOException;

public class PreDownsampleReduceHourJob extends
		TableReducer<ImmutableBytesWritable, KeyValue, ImmutableBytesWritable> {

	@Override
	protected void setup(Context context) throws IOException,
			InterruptedException {
	}

	@Override
	public void reduce(ImmutableBytesWritable key, Iterable<KeyValue> values,
			Context context) throws IOException, InterruptedException {
		
		Configuration conf = context.getConfiguration();
		String readname = conf.get("tablename");
		String[] splitTablename = readname.split("\\.");
		String tablename = splitTablename[0] + PreDownsampleUtil.MAPREDUCE_PD + "." + splitTablename[1];
		
		Append appendMax = null, appendMin = null, appendSum = null, appendDev = null, appendCount = null,
				appendFirst = null, appendPercent = null;
		
		HTable table = new HTable(conf, tablename);
		double max = Double.MIN_VALUE, min = Double.MAX_VALUE, 
				sum = 0, dev = 0, count = 0;
		double first_time = Double.MAX_VALUE, first_value = 0;
		double percent_a = 0, percent_b = 0;
		
		PreDownsampleUtil.DownsampleType downsample = new PreDownsampleUtil.DownsampleType();
		byte[] tsid = new byte[8];
		byte[] mid = new byte[4];
		byte[] hourOfBase = new byte[]{0,0};
		byte[] hourOfDay = new byte[]{0};
		for( KeyValue kv : values ) {
			byte[] Key = kv.getRow();
			byte[] Value = kv.getValue();
			byte t = Key[Key.length-1];
			System.arraycopy(Key, 0, mid, 0, 4);
			System.arraycopy(Key, 8, tsid, 0, 8);
			System.arraycopy(Key, 4, hourOfBase, 0, 2);
			System.arraycopy(Key, 6, hourOfDay, 0, 1);
			
			switch(t) {
			case PreDownsampleUtil.DownsampleType.MAX:
				{
					downsample.type = PreDownsampleUtil.DownsampleType.MAX;
					byte type = Value[2];
					if( type == PreDownsampleUtil.SaveType.ORIGIN || type == PreDownsampleUtil.SaveType.SINGLE ) {
						for( int i=1; i<Value.length; i+=10 ) {
							byte[] b = new byte[8];
							System.arraycopy(Value, i+2, b, 0, 8);
							double com = Bytes.toDouble(b);
							if( com > max ) {
								max = com;
							}
						}
					}
				}
				break;
			case PreDownsampleUtil.DownsampleType.MIN:
				{
					downsample.type = PreDownsampleUtil.DownsampleType.MIN;
					byte type = Value[2];
					if( type == PreDownsampleUtil.SaveType.ORIGIN || type == PreDownsampleUtil.SaveType.SINGLE ) {
						for( int i=1; i<Value.length; i+=10 ) {
							byte[] b = new byte[8];
							System.arraycopy(Value, i+2, b, 0, 8);
							double com = Bytes.toDouble(b);
							if( com < min ) {
								min = com;
							}
						}
					}
				}
				break;
			case PreDownsampleUtil.DownsampleType.SUM:
				{
					downsample.type = PreDownsampleUtil.DownsampleType.SUM;
					byte type = Value[2];
					if( type == PreDownsampleUtil.SaveType.ORIGIN || type == PreDownsampleUtil.SaveType.SINGLE ) {
						for( int i=1; i<Value.length; i+=10 ) {
							byte[] b = new byte[8];
							System.arraycopy(Value, i+2, b, 0, 8);
							double com = Bytes.toDouble(b);
							sum += com;
						}
					}
				}
				break;
			case PreDownsampleUtil.DownsampleType.DEV:
				{
					downsample.type = PreDownsampleUtil.DownsampleType.DEV;
					byte type = Value[2];
					if( type == PreDownsampleUtil.SaveType.ORIGIN || type == PreDownsampleUtil.SaveType.SINGLE ) {
						for( int i=1; i<Value.length; i+=10 ) {
							byte[] b = new byte[8];
							System.arraycopy(Value, i+2, b, 0, 8);
							double com = Bytes.toDouble(b);
							sum += com;
							count ++;
						}
					}
				}
				break;
			case PreDownsampleUtil.DownsampleType.COUNT:
				{
					downsample.type = PreDownsampleUtil.DownsampleType.COUNT;
					byte type = Value[2];
					if( type == PreDownsampleUtil.SaveType.ORIGIN || type == PreDownsampleUtil.SaveType.SINGLE ) {
						for( int i=1; i<Value.length; i+=10 ) {
							byte[] b = new byte[8];
							System.arraycopy(Value, i+2, b, 0, 8);
							count ++;
						}
					}
				}
				break;
			case PreDownsampleUtil.DownsampleType.FIRST:
				{
					downsample.type = PreDownsampleUtil.DownsampleType.FIRST;
					byte time = Value[1];
					byte type = Value[2];
					if( type == PreDownsampleUtil.SaveType.ORIGIN || type == PreDownsampleUtil.SaveType.SINGLE ) {
						for( int i=1; i<Value.length; i+=10 ) {
							byte[] b = new byte[8];
							System.arraycopy(Value, i+2, b, 0, 8);
							double com = Bytes.toDouble(b);
							byte offset = Value[i];
							int kOffset = (int)((int)time*240) + offset;
							if( first_time > kOffset ) {
								first_value = com;
							}
						}
					}
				}
				break;
			case PreDownsampleUtil.DownsampleType.PERCENT:
				{
					downsample.type = PreDownsampleUtil.DownsampleType.PERCENT;
					byte type = Value[2];
					if( type == PreDownsampleUtil.SaveType.PERCENT ) {
						for( int i=1; i<Value.length; i+=18 ) {
							byte[] a = new byte[8];
							byte[] b = new byte[8];
							System.arraycopy(Value, i+2, a, 0, 8);
							System.arraycopy(Value, i+8, b, 9, 8);
							
							double com1 = Bytes.toDouble(a);
							double com2 = Bytes.toDouble(b);
							percent_a += com1;
							percent_b += com2;
						}
					}
				}
				break;
			default:
				break;
			}
		}
		
		byte[] keyToWrite = new byte[16];
		System.arraycopy(mid, 0, keyToWrite, 0, 4);
		System.arraycopy(hourOfBase, 0, keyToWrite, 4, 2);
		System.arraycopy(Bytes.toBytes(downsample.type), 0, keyToWrite, 6, 1);
		System.arraycopy(Bytes.toBytes(PreDownsampleUtil.IntervalType.HOUR), 0, keyToWrite, 7, 1);
		System.arraycopy(tsid, 0, keyToWrite, 8, 8);

		System.arraycopy(Bytes.toBytes(PreDownsampleUtil.DownsampleType.MAX), 0, keyToWrite, 6, 1);
		appendMax = new Append(keyToWrite);
		System.arraycopy(Bytes.toBytes(PreDownsampleUtil.DownsampleType.MIN), 0, keyToWrite, 6, 1);
		appendMin = new Append(keyToWrite);
		System.arraycopy(Bytes.toBytes(PreDownsampleUtil.DownsampleType.SUM), 0, keyToWrite, 6, 1);
		appendSum = new Append(keyToWrite);
		System.arraycopy(Bytes.toBytes(PreDownsampleUtil.DownsampleType.DEV), 0, keyToWrite, 6, 1);
		appendDev = new Append(keyToWrite);
		System.arraycopy(Bytes.toBytes(PreDownsampleUtil.DownsampleType.COUNT), 0, keyToWrite, 6, 1);
		appendCount = new Append(keyToWrite);
		System.arraycopy(Bytes.toBytes(PreDownsampleUtil.DownsampleType.FIRST), 0, keyToWrite, 6, 1);
		appendFirst = new Append(keyToWrite);
		System.arraycopy(Bytes.toBytes(PreDownsampleUtil.DownsampleType.PERCENT), 0, keyToWrite, 6, 1);
		appendPercent = new Append(keyToWrite);
		
		byte[] a = Bytes.toBytes(percent_a);
		byte[] b = Bytes.toBytes(percent_b);
		byte[] a_b = new byte[16];
		System.arraycopy(a, 0, a_b, 0, 8);
		System.arraycopy(b, 0, a_b, 8, 8);
		
		int column = hourOfDay[0];
		appendMax.add(PreDownsampleUtil.COLUMN_FAMILY.getBytes(), Integer.toString(column).getBytes(), Bytes.toBytes(max));
		appendMin.add(PreDownsampleUtil.COLUMN_FAMILY.getBytes(), Integer.toString(column).getBytes(), Bytes.toBytes(min));
		appendSum.add(PreDownsampleUtil.COLUMN_FAMILY.getBytes(), Integer.toString(column).getBytes(), Bytes.toBytes(sum));
		appendDev.add(PreDownsampleUtil.COLUMN_FAMILY.getBytes(), Integer.toString(column).getBytes(), Bytes.toBytes(dev));
		appendCount.add(PreDownsampleUtil.COLUMN_FAMILY.getBytes(), Integer.toString(column).getBytes(), Bytes.toBytes(count));
		appendFirst.add(PreDownsampleUtil.COLUMN_FAMILY.getBytes(), Integer.toString(column).getBytes(), Bytes.toBytes(first_value));
		appendPercent.add(PreDownsampleUtil.COLUMN_FAMILY.getBytes(), Integer.toString(column).getBytes(), a_b);
		
		table.append(appendMax);
		table.append(appendMin);
		table.append(appendSum);
		table.append(appendDev);
		table.append(appendCount);
		table.append(appendFirst);
		table.append(appendPercent);
		table.close();
		
		int hours = (int) ((System.currentTimeMillis()/(24*3600000)-1)*24);
		
//		int f = Bytes.toShort(hourOfBase) + hourOfDay[0];
		int f = hours + hourOfDay[0];
		HTablePool htablePool = PreDownsampleUtil.initTablePool(conf.get("hbase.zookeeper.quorum"),conf.get("ZnodePath"));
		HTableInterface htable = htablePool.getTable(PreDownsampleUtil.DASHBOARD_PREDOWNSAMPLE_TIMERANGE);
		if( htable != null ) {
			Get get = new Get(mid);
			Result result = htable.get(get);
			Put put = null;
			if( result != null ) {
				byte[] cs = result.getValue(PreDownsampleUtil.COLUMN_FAMILY_C.getBytes(), PreDownsampleUtil.COLUMN_S.getBytes());
				byte[] ce = result.getValue(PreDownsampleUtil.COLUMN_FAMILY_C.getBytes(), PreDownsampleUtil.COLUMN_E.getBytes());
				if( cs == null ) {
					if( put == null ) {
						put = new Put(mid);
					}
					put.add(PreDownsampleUtil.COLUMN_FAMILY_C.getBytes(), PreDownsampleUtil.COLUMN_S.getBytes(),
							Bytes.toBytes(f));
				} else {
					int cStart = Bytes.toInt(cs);
					if( cStart > f ) {
						if( put == null ) {
							put = new Put(mid);
						}
						put.add(PreDownsampleUtil.COLUMN_FAMILY_C.getBytes(), PreDownsampleUtil.COLUMN_S.getBytes(),
								Bytes.toBytes(f));
					}
				}
				
				if( ce == null ) {
					if( put == null ) {
						put = new Put(mid);
					}
					put.add(PreDownsampleUtil.COLUMN_FAMILY_C.getBytes(), PreDownsampleUtil.COLUMN_E.getBytes(),
							Bytes.toBytes(f+1));
				} else {
					int cEnd = Bytes.toInt(ce);
					if( cEnd < f+1 ) {
						if( put == null ) {
							put = new Put(mid);
						}
						put.add(PreDownsampleUtil.COLUMN_FAMILY_C.getBytes(), PreDownsampleUtil.COLUMN_E.getBytes(),
								Bytes.toBytes(f+1));
					}
				}
			} else {
				put = new Put(mid);
				put.add(PreDownsampleUtil.COLUMN_FAMILY_C.getBytes(), PreDownsampleUtil.COLUMN_S.getBytes(),
						Bytes.toBytes(f));
				put.add(PreDownsampleUtil.COLUMN_FAMILY_C.getBytes(), PreDownsampleUtil.COLUMN_S.getBytes(),
						Bytes.toBytes(f+1));
			}
			
			if( put != null ) {
				htable.put(put);
			} 
			HBaseClientUtil.closeHTable(htable);
		}
	}
}

