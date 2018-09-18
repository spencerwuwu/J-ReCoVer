//https://searchcode.com/file/116093382/examples/sensors/src/main/java/com/mongodb/hadoop/examples/sensors/LogCombiner.java#l-9

package reduce_test;

import java.io.IOException;
import java.util.Iterator;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.Reporter;

public class collector0_90_5_6_2 implements ReducerO<Text, IntWritable, Text, IntWritable> {

	public static void main(String[] args){
		for(int i=0; i<Common.maxcount ; i++){  
			ReducerO<Text, IntWritable, Text, IntWritable> reducer=new collector0_90_5_6_2();
			Tester<Text, IntWritable, Text, IntWritable> tester=new Tester<Text, IntWritable, Text, IntWritable>();
			IntWritable[] solutionArray = { new IntWritable(1), new IntWritable(2), new IntWritable(3) };
			try {
				tester.test(new Text("key"), solutionArray, reducer);
			} catch (IOException | InterruptedException e1) {
				e1.printStackTrace();
			}
		}
	}  

	public void reduce(final Text key, final Iterator<IntWritable> values, final OutputCollector<Text, IntWritable> output,
			final Reporter reporter) throws IOException {
		int count = 0;
		while (values.hasNext()) {
			count += values.next().get();
		}

		output.collect(key, new IntWritable(count));
	}
}
