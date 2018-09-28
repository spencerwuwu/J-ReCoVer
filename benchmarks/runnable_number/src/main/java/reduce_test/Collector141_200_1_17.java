//https://searchcode.com/file/100326439/hadoop-mapreduce-project/hadoop-mapreduce-client/hadoop-mapreduce-client-jobclient/src/test/java/org/apache/hadoop/mapred/TestOldCombinerGrouping.java#l-56

package reduce_test;

import java.io.IOException;
import java.util.Iterator;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.Reporter;

public class Collector141_200_1_17 implements ReducerO<Text, LongWritable, Text, LongWritable> {

	public static void main(String[] args){
		for(int i=0; i<Common.maxcount ; i++){  
			ReducerO<Text, LongWritable, Text, LongWritable> reducer=new Collector141_200_1_17();
			Tester<Text, LongWritable, Text, LongWritable> tester=new Tester<Text, LongWritable, Text, LongWritable>();
			LongWritable[] solutionArray = { new LongWritable(1), new LongWritable(2), new LongWritable(3) };
			try {
				tester.test(new Text("key"), solutionArray, reducer);
			} catch (IOException | InterruptedException e1) {
				e1.printStackTrace();
			}
		}
	}

	public void reduce(Text key, Iterator<LongWritable> values,
			OutputCollector<Text, LongWritable> output, Reporter reporter)
					throws IOException {
		LongWritable maxValue = null;
		while (values.hasNext()) {
			LongWritable value = values.next();
			if (maxValue == null) {
				maxValue = value;
			} else if (value.compareTo(maxValue) > 0) {
				maxValue = value;
			}
		}
		output.collect(key, maxValue);
	}

}
