//https://searchcode.com/file/11331165/core/src/main/java/org/apache/mahout/math/hadoop/stats/StandardDeviationCalculatorReducer.java#l-25

package reduce_test;

import java.io.IOException;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.IntWritable;

public class context0_90_35_20 implements ReducerC<IntWritable, DoubleWritable>{

	public static void main(String[] args){
		for(int i=0; i<Common.maxcount ; i++){  
			ReducerC<IntWritable, DoubleWritable> reducer=new context0_90_35_20();
			Tester<IntWritable, DoubleWritable, IntWritable, DoubleWritable> tester=new Tester<IntWritable, DoubleWritable, IntWritable, DoubleWritable>();
			DoubleWritable[] solutionArray = { new DoubleWritable(1.5), new DoubleWritable(2.5), new DoubleWritable(3.5) };
			try {
				tester.test(new IntWritable(1), solutionArray, reducer);
			} catch (IOException | InterruptedException e1) {
				e1.printStackTrace();
			}
		}
	}

	public void reduce(IntWritable key, Iterable<DoubleWritable> values,
			Context context) throws IOException, InterruptedException {
		double sum = 0.0;
		for (DoubleWritable value : values) {
			sum += value.get();
		}
		context.write(key, new DoubleWritable(sum));
	}

}