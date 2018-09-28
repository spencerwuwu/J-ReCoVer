//https://searchcode.com/file/74337084/mapreduce-lab/solved/src/fr/eurecom/dsg/mapreduce/WordCountComplex.java#l-19

package reduce_test;

import java.io.IOException;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;

public class context201_270_28_6 implements ReducerC<Text, IntWritable>{

	public static void main(String[] args){
		for(int i=0; i<Common.maxcount ; i++){  
			ReducerC<Text, IntWritable> reducer=new context201_270_28_6();
			Tester<Text, IntWritable, Text, IntWritable> tester=new Tester<Text, IntWritable, Text, IntWritable>();
			IntWritable[] solutionArray = { new IntWritable(1), new IntWritable(2), new IntWritable(3) };
			try {
				tester.test(new Text("key"), solutionArray, reducer);
			} catch (IOException | InterruptedException e1) {
				e1.printStackTrace();
			}
		}
	}

	public void reduce(Text text, Iterable<IntWritable> iterable, Context context)
			throws IOException, InterruptedException {

		int result = 0;

		for (IntWritable iterator : iterable)
			result += iterator.get();

		context.write(text, new IntWritable(result));
	}

}
