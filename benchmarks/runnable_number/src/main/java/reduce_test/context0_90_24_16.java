//https://searchcode.com/file/66797561/src/main/java/com/jayway/hadoop/gutenberg/RowLengthCounter.java#l-8

package reduce_test;

import java.io.IOException;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;

public class context0_90_24_16 implements ReducerC<Text, IntWritable>{

	public static void main(String[] args){
		for(int i=0; i<Common.maxcount ; i++){  
			ReducerC<Text, IntWritable> reducer=new context0_90_24_16();
			Tester<Text, IntWritable, Text, IntWritable> tester=new Tester<Text, IntWritable, Text, IntWritable>();
			IntWritable[] solutionArray = { new IntWritable(1), new IntWritable(2), new IntWritable(3) };
			try {
				tester.test(new Text("key"), solutionArray, reducer);
			} catch (IOException | InterruptedException e1) {
				e1.printStackTrace();
			}
		}
	}

	/*
	 * This method should calculate the maximum row length for each file 
	 */
	public void reduce(Text fileName, Iterable<IntWritable> arg1, Context context) throws IOException ,InterruptedException {


		int maxValue = Integer.MIN_VALUE;

		//TODO: calculate the maximum row length per fileName

		context.write(fileName, new IntWritable(maxValue));
	};

}
