//https://searchcode.com/file/68758556/src/org/manea/vlad/assignedmixcounter/AssignedMixCounterReducer.java#l-10

package reduce_test;

import java.io.IOException;
import java.util.Iterator;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.mapred.Reporter;

public class collector0_90_3_2 implements ReducerO<IntWritable, IntWritable, IntWritable, IntWritable> {

	public static void main(String[] args){
		for(int i=0; i<Common.maxcount ; i++){  
			ReducerO<IntWritable, IntWritable, IntWritable, IntWritable> reducer=new collector0_90_3_2();
			Tester<IntWritable, IntWritable, IntWritable, IntWritable> tester=new Tester<IntWritable, IntWritable, IntWritable, IntWritable>();
			IntWritable[] solutionArray = { new IntWritable(1), new IntWritable(2), new IntWritable(3) };
			try {
				tester.test(new IntWritable(1), solutionArray, reducer);
			} catch (IOException | InterruptedException e1) {
				e1.printStackTrace();
			}
		}
	}

	public void reduce(IntWritable key, Iterator<IntWritable> values,
			OutputCollector<IntWritable, IntWritable> output, Reporter reporter)
					throws IOException {

		// initialize sum value
		int counter = 0;

		// iterate all values in iterator
		while (values.hasNext()) {

			// add count to sum
			counter += values.next().get();

		}

		// output (mix, count(mix))
		output.collect(key, new IntWritable(counter));

	}

}
