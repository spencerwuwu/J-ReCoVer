// https://searchcode.com/api/result/110936010/

package joins.multiway;

import java.io.IOException; 
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import joins.twoway.TextPair;
import org.apache.hadoop.io.*;
import org.apache.hadoop.mapred.*;

@SuppressWarnings("deprecation")
public class RepartitionReducer extends MapReduceBase implements Reducer<TextPair, TextPair, Text, Text> {

	//ArrayList<String> tables[] = new ArrayList<String>[5];
	ArrayList tables[] = null;
	//HashMap<String, String> tagMap = new HashMap<String, String>();
	int no_of_tables = 0;
	
	@Override
	public void configure(JobConf conf) {
		super.configure(conf);
		String[] tags = conf.get("tables.tags").split(";");		
		no_of_tables = tags.length/2;
		
		//TODO : Buffer all but the last table. Stream the last table
		tables = new ArrayList[no_of_tables];
		for(int i=0; i< no_of_tables;i++)
		{
			tables[i] = new ArrayList<String>();
		}
	}

	@Override
	public void reduce(TextPair key, Iterator<TextPair> values, 
			OutputCollector<Text, Text> output,  Reporter reporter)
			throws IOException {
		TextPair value = null;
		
		for(int i=0;i<tables.length;i++)
			tables[i].clear();
		
		while(values.hasNext())
		{
			value = values.next();
			int tag = Integer.parseInt(value.getSecond().toString());
			tables[tag].add(value.getFirst().toString());
		}
		
		String[] partialList = new String[tables.length];
		joinAndCollect(tables, 0, partialList, key, output, reporter);
	}
	
	private void joinAndCollect(ArrayList[] values,
			int pos, String[] partialList, TextPair key,
			OutputCollector<Text, Text> output, Reporter reporter) throws IOException {

		if (values.length == pos) {
			output.collect(key.getFirst(), new Text(combine(partialList)));
			return;
		}
		ArrayList<String> nextValues = values[pos];
		//nextValues.reset();
		//while (nextValues.hasNext()) {
		for(String s : nextValues)
		{
			//Object v = nextValues.next();
			//partialList.append(s+" ");
			partialList[pos] = s;
			joinAndCollect(values, pos + 1, partialList, key, output, reporter);
			//partialList.delete(start, end)
		}
	}
	String combine(String[] values)
	{
		StringBuilder sb = new StringBuilder();
		for(String s : values)
		{
			sb.append(s + " ");
		}
		return sb.toString();
	}
}

