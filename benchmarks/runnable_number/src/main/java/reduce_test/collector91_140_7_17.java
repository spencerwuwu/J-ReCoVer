//https://searchcode.com/file/65663781/src/dist/edu/umd/cloud9/example/memcached/demo/WordCount.java#l-25

package reduce_test;

import java.io.IOException;
import java.util.Iterator;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.Reporter;

public class collector91_140_7_17 implements ReducerO<Text, IntWritable, Text, IntWritable> {

	public static void main(String[] args){
		for(int i=0; i<Common.maxcount ; i++){  
			ReducerO<Text, IntWritable, Text, IntWritable> reducer=new  collector91_140_7_17();
			Tester<Text, IntWritable, Text, IntWritable> tester=new Tester<Text, IntWritable, Text, IntWritable>();
			IntWritable[] solutionArray = { new IntWritable(1), new IntWritable(2), new IntWritable(3) };
			try {
				tester.test(new Text("key"), solutionArray, reducer);
			} catch (IOException | InterruptedException e1) {
				e1.printStackTrace();
			}
		}
	}  

	private final static IntWritable SumValue = new IntWritable();

	public void reduce(Text key, Iterator<IntWritable> values,
			OutputCollector<Text, IntWritable> output, Reporter reporter) throws IOException {
		// sum up values
		int sum = 0;
		while (values.hasNext()) {
			sum += values.next().get();
		}
		SumValue.set(sum);
		output.collect(key, SumValue);
	}	  
}