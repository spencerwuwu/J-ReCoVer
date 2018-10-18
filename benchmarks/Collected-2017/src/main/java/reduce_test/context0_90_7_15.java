//https://searchcode.com/file/96118988/java/hadoop/MaxTemperatureReducer.java#l-5

package reduce_test;

import java.io.IOException;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;

public class context0_90_7_15 implements ReducerC<Text, IntWritable>{

	public static void main(String[] args){
		for(int i=0; i<Common.maxcount ; i++){  
			ReducerC<Text, IntWritable> reducer=new context0_90_7_15();
			Tester<Text, IntWritable, Text, IntWritable> tester=new Tester<Text, IntWritable, Text, IntWritable>();
			IntWritable[] solutionArray = { new IntWritable(1), new IntWritable(2), new IntWritable(3) };
			try {
				tester.test(new Text("key"), solutionArray, reducer);
			} catch (IOException | InterruptedException e1) {
				e1.printStackTrace();
			}
		}
	}

	public void reduce(Text key, Iterable<IntWritable> values, Context context) throws IOException, InterruptedException {
		int maxValue = Integer.MIN_VALUE;
		for(IntWritable value : values){
			maxValue = Math.max(maxValue, value.get());
		}
		context.write(key,new IntWritable(maxValue));
	}

}
