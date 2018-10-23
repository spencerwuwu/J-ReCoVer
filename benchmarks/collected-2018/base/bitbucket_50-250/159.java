// https://searchcode.com/api/result/134047298/

/**
 * 
 */
package wikiParser.mapReduce.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.MapReduceBase;
import org.apache.hadoop.mapred.OutputCollector;
import org.apache.hadoop.mapred.Reducer;
import org.apache.hadoop.mapred.Reporter;

public class HistogramReduce extends MapReduceBase implements Reducer<Text, Text, Text, Text> {
	public void reduce(Text key, Iterator<Text> values, OutputCollector<Text,Text> output, 
			Reporter reporter) throws IOException {
		/*
		 * Outputs stats:
		 * min, max, mean, sum, first quartile, median, third quartile
		 */
		ArrayList<Double> valueList = new ArrayList<Double>();
		double min=65626, max=0, mean=-1, sum=0, count=0, fQuart=-1, median=-1, tQuart=-1;
		while (values.hasNext()) {
			double value = Double.parseDouble(values.next().toString());
//			valueList.add(value);
			count += 1;
			if (value < min) {
				min = value;
			}
			if (value > max) {
				max = value;
			}
			sum += value;
		}
		mean = sum / count;
		
//		Object[] sortedValues = valueList.toArray();
//		Arrays.sort(sortedValues);
//		int medianIndex = (int)Math.ceil(((double)sortedValues.length)/2);
//		//median
//		if (sortedValues.length % 2 == 0) {
//			median = ((((Double) sortedValues[medianIndex]).doubleValue()+
//					((Double) sortedValues[medianIndex+1]).doubleValue())/2);
//		}
//		else {
//			median = ((Double) sortedValues[medianIndex]).doubleValue();
//		}
//		//first quartile
//		int fQuartIndex = (int)Math.ceil(((double)medianIndex)/2);
//		if (medianIndex % 2 == 0) {
//			fQuart = ((((Double) sortedValues[fQuartIndex]).doubleValue()+
//					((Double) sortedValues[fQuartIndex+1]).doubleValue())/2);
//		}
//		else {
//			fQuart = ((Double) sortedValues[fQuartIndex]).doubleValue();
//		}
//		//third quartile
//		int tQuartIndex = (int)Math.ceil((sortedValues.length-(double)medianIndex)/2);
//		if ((sortedValues.length-medianIndex) % 2 == 0) {
//			tQuart = ((((Double) sortedValues[tQuartIndex]).doubleValue()+
//					((Double) sortedValues[tQuartIndex+1]).doubleValue())/2);
//		}
//		else {
//			tQuart = ((Double) sortedValues[tQuartIndex]).doubleValue();
//		}
//		min = (Double) sortedValues[0];
//		max = (Double) sortedValues[-1];
		output.collect(new Text("min"), new Text(Double.toString(min)));
		output.collect(new Text("max"), new Text(Double.toString(max)));
		output.collect(new Text("count"), new Text(Double.toString(count)));
		output.collect(new Text("sum"), new Text(Double.toString(sum)));
		output.collect(new Text("mean"), new Text(Double.toString(mean)));
		output.collect(new Text("fQuart"), new Text(Double.toString(fQuart)));
		output.collect(new Text("median"), new Text(Double.toString(median)));
		output.collect(new Text("tQuart"), new Text(Double.toString(tQuart)));
	}
}
