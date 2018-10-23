// https://searchcode.com/api/result/64481681/

import java.io.IOException;
import java.util.Map.Entry;

import org.apache.accumulo.core.Constants;
import org.apache.accumulo.core.client.BatchWriter;
import org.apache.accumulo.core.client.Connector;
import org.apache.accumulo.core.client.Scanner;
import org.apache.accumulo.core.client.ZooKeeperInstance;
import org.apache.accumulo.core.data.Key;
import org.apache.accumulo.core.data.Mutation;
import org.apache.accumulo.core.data.Range;
import org.apache.accumulo.core.data.Value;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;

public class Step1Reducer extends Reducer<Text,Text,Text,Text> {
	
//	private MultipleOutputs mos;
//	 public void setup(Context context) {
//	 mos = new MultipleOutputs(context);
//	 }
	 
	static String user = "aditm";
	static byte[] pass = "aditm".getBytes();
	static String instanceName = "accumulo";
	static String zooKeepers = "zk2:2181,zk3:2181,zk4:2181";
	static long memBuf = 1000000L; // bytes to store before sending a batch
	static long timeout = 1000L; // milliseconds to wait before sending
	static int numThreads = 10;
	
	static String table4s = "aditm_tb4_incr";
	Text colFam = new Text("pathColFam");
	Text colQual = new Text("pathColQual");
	
	private Text tValue = new Text();
	public void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
		
		try {
			ZooKeeperInstance instance = new ZooKeeperInstance(instanceName, zooKeepers);
			Connector connector = instance.getConnector(user, pass);
			BatchWriter writer = connector.createBatchWriter(table4s, memBuf, timeout, numThreads);
			
			if(key.toString().equalsIgnoreCase(dynamic_fs_stats.unknownBlock)) {
				//Write unknown blockName entries as it is
				for (Text val : values) {
					context.write(key, val);
				}
			} else {
				//Merge on block name and add read sizes
				RowValue rv = dynamic_fs_stats.mergeAllValues(values, false);
				if(rv.path != dynamic_fs_stats.unknownPath && rv.blockName != dynamic_fs_stats.unknownBlock) {
					//Write block to path mapping in db
					Mutation mutation = new Mutation(new Text(rv.blockName)); 
					mutation.put(colFam, colQual, new Value(rv.path.getBytes()));
					writer.addMutation(mutation);
					
					//Write to file
					tValue.set(rv.toString());
					context.write(key, tValue);
				} else if (rv.blockName != dynamic_fs_stats.unknownBlock) {
					//Query db for path corresponding to file
					Scanner scan = connector.createScanner(table4s, Constants.NO_AUTHS);
					scan.setRange(new Range(rv.blockName,rv.blockName));
					//scan.fetchColumnFamily(fam);
					 
					for(Entry<Key,Value> e : scan) {
					    //String row = e.getKey().getRow();
					    String path = e.getValue().toString();
					    if(path.length() > 0) {
					    	rv.path = path;
					    }
					}
					
					if(rv.path != dynamic_fs_stats.unknownPath) {
						System.out.println("Found path in db:" + rv.path);
						
						tValue.set(rv.toString());
						context.write(key, tValue);
					}
				}
			}
			
			writer.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
//	 public void cleanup(Context c) throws IOException {
//		 try {
//			mos.close();
//		} catch (InterruptedException e) {
//			e.printStackTrace();
//		}
//	}
}

