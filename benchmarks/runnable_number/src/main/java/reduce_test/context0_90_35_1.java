//https://searchcode.com/file/2223594/core/src/main/java/org/apache/mahout/fpm/pfpgrowth/ParallelCountingReducer.java#l-23

package reduce_test;

import java.io.IOException;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;

public class context0_90_35_1 implements ReducerC<Text, LongWritable>{

	public static void main(String[] args){
		for(int i=0; i<Common.maxcount ; i++){  
			ReducerC<Text, LongWritable> reducer=new context0_90_35_1();
			Tester<Text, LongWritable, Text, LongWritable> tester=new Tester<Text, LongWritable, Text, LongWritable>();
			LongWritable[] solutionArray = { new LongWritable(1), new LongWritable(2), new LongWritable(3) };
			try {
				tester.test(new Text("key"), solutionArray, reducer);
			} catch (IOException | InterruptedException e1) {
				e1.printStackTrace();
			}
		}
	}

	public void reduce(Text key, Iterable<LongWritable> values, Context context) throws IOException,
	InterruptedException {
		long sum = 0;
		for (LongWritable value : values) {
			//context.setStatus("Parallel Counting Reducer :" + key);
			sum += value.get();
		}
		//context.setStatus("Parallel Counting Reducer: " + key + " => " + sum);
		context.write(key, new LongWritable(sum));
	}

}
