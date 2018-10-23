//https://searchcode.com/file/68682372/Examples/src/com/spbsu/hadoop/FirstLetterCounter.java#l-11

package reduce_test;

import java.io.IOException;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;

public class context141_200_30_8 implements ReducerC<Text, IntWritable>{

	public static void main(String[] args){
		for(int i=0; i<Common.maxcount ; i++){  
			ReducerC<Text, IntWritable> reducer=new context141_200_30_8();
			Tester<Text, IntWritable, Text, IntWritable> tester=new Tester<Text, IntWritable, Text, IntWritable>();
			IntWritable[] solutionArray = { new IntWritable(1), new IntWritable(2), new IntWritable(3) };
			try {
				tester.test(new Text("key"), solutionArray, reducer);
			} catch (IOException | InterruptedException e1) {
				e1.printStackTrace();
			}
		}
	}

	static int j = 0;

	private static int k = 0;
	private final static int KEY_NUMBER = 500000;

	public void reduce(Text key, Iterable<IntWritable> values, Context context) throws IOException, InterruptedException {
		int delay = 1;
		int sum = 0;
		for (IntWritable i: values){
			sum += i.get();
			if (k * KEY_NUMBER < j){
				Thread.sleep(delay * 1000);
				k++;
			}
			j+=i.get();

		}
		//	            Thread.sleep(3 + sum / delay);
		context.write(key, new IntWritable(sum));
	}

}
