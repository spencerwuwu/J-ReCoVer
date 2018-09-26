//https://searchcode.com/file/93068126/src/mapred/org/apache/hadoop/mapred/lib/IdentityReducer.java#l-27

package reduce_test;

import java.io.IOException;
import java.util.Iterator;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.mapred.Reporter;

public class Collector11_0_90_6_4 implements ReducerO<LongWritable, LongWritable, LongWritable, LongWritable> {

	public static void main(String[] args){
		for(int i=0; i<Common.maxcount ; i++){  
			ReducerO<LongWritable, LongWritable, LongWritable, LongWritable> reducer=new Collector11_0_90_6_4();
			Tester<LongWritable, LongWritable, LongWritable, LongWritable> tester=new Tester<LongWritable, LongWritable, LongWritable, LongWritable>();
			LongWritable[] solutionArray = { new LongWritable(1), new LongWritable(2), new LongWritable(3) };
			try {
				tester.test(new LongWritable(1), solutionArray, reducer);
			} catch (IOException | InterruptedException e1) {
				e1.printStackTrace();
			}
		}
	} 

	/** Writes all keys and values directly to output. */
	public void reduce(LongWritable key, Iterator<LongWritable> values,
			OutputCollector<LongWritable, LongWritable> output, Reporter reporter)
					throws IOException {
		while (values.hasNext()) {
			output.collect(key, values.next());
		}
	}

}