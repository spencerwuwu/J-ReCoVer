// https://searchcode.com/api/result/44808748/

package com.search.indexing;

import java.io.IOException;
import java.util.Iterator;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.MapReduceBase;
import org.apache.hadoop.mapred.OutputCollector;
import org.apache.hadoop.mapred.Reducer;
import org.apache.hadoop.mapred.Reporter;

/**
 * Reduce Class for creating an Inverted Index. 
 * The input format is - 
 * Key - <word> 
 * Value - List of (PageId,WordLocation) 
 * The Reduce Output is 
 * Key - <word> -
 * Value - PageId,WordLocation ^^ PageId,WordLocation ^^ PageId,WordLocation 
 * e.g. Key - Middle
 * Value - Types_of_Schools,1 ^^ Units_of_MeasureMent, 88 ^^ Tale_of_Two_Cities: 700
 * 
 */
public class InvertedIndexReduce extends MapReduceBase implements
		Reducer<Text, Text, Text, Text> {
	private Text index = new Text();

	public void reduce(Text key, Iterator<Text> value,
			OutputCollector<Text, Text> output, Reporter reporter)
			throws IOException {
		StringBuilder outValue = new StringBuilder();
		String pre = new String();
		boolean first = true;
		while (value.hasNext()) {
			String nextStr = value.next().toString();
			if (!nextStr.equals(pre)) {
				pre = nextStr;
				if (first) {
					first = false;
				} else {
					outValue.append("^^");
				}
				outValue.append(nextStr);

			}
		}

		index.set(outValue.toString());
		output.collect(key, index);
	}

}
