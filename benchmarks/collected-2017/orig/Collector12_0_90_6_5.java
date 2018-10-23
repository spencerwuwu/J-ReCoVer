//https://searchcode.com/file/93068158/src/mapred/org/apache/hadoop/mapred/lib/LongSumReducer.java#l-26

package reduce_test;

import java.io.IOException;
import java.util.Iterator;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.mapred.Reporter;

public class Collector12_0_90_6_5 implements ReducerO<LongWritable, LongWritable, LongWritable, LongWritable> {

	public static void main(String[] args){
		for(int i=0; i<Common.maxcount ; i++){  
			ReducerO<LongWritable, LongWritable, LongWritable, LongWritable> reducer=new Collector12_0_90_6_5();
			Tester<LongWritable, LongWritable, LongWritable, LongWritable> tester=new Tester<LongWritable, LongWritable, LongWritable, LongWritable>();
			LongWritable[] solutionArray = { new LongWritable(1), new LongWritable(2), new LongWritable(3) };
			try {
				tester.test(new LongWritable(1), solutionArray, reducer);
			} catch (IOException | InterruptedException e1) {
				e1.printStackTrace();
			}
		}
	} 

	public void reduce(LongWritable key, Iterator<LongWritable> values,
			OutputCollector<LongWritable, LongWritable> output,
			Reporter reporter)
					throws IOException {

		// sum all values for this key
		long sum = 0;
		while (values.hasNext()) {
			sum += values.next().get();
		}

		// output sum
		output.collect(key, new LongWritable(sum));
	}

}