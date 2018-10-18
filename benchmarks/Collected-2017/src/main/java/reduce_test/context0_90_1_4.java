//https://searchcode.com/file/95086566/Lab1-Task2%20and%203/WordCount/WordCount.java#l-9

package reduce_test;

import java.io.IOException;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;

public class context0_90_1_4 implements ReducerC<Text, IntWritable>{

	public static void main(String[] args){
		for(int i=0; i<Common.maxcount ; i++){  
			ReducerC<Text, IntWritable> reducer=new context0_90_1_4();
			Tester<Text, IntWritable, Text, IntWritable> tester=new Tester<Text, IntWritable, Text, IntWritable>();
			IntWritable[] solutionArray = { new IntWritable(1), new IntWritable(2), new IntWritable(3) };
			try {
				tester.test(new Text("key"), solutionArray, reducer);
			} catch (IOException | InterruptedException e1) {
				e1.printStackTrace();
			}
		}
	}

	private IntWritable result = new IntWritable();

	public void reduce(Text key, Iterable<IntWritable> values, 
            Context context) throws IOException, InterruptedException {
		int sum = 0; // initialize the sum for each keyword
		for (IntWritable val : values) {
			sum += val.get();  
		}
		result.set(sum);

		context.write(key, result); // create a pair <keyword, number of occurences>
	}

}
