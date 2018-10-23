//https://searchcode.com/file/69738596/code/src/main/java/com/thoughtworks/samples/hadoop/mapred/wordcount/WCReducer.java#l-4

package reduce_test;

import java.io.IOException;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;

public class context0_90_10_13 implements ReducerC<Text, IntWritable>{

	public static void main(String[] args){
		for(int i=0; i<Common.maxcount ; i++){  
			ReducerC<Text, IntWritable> reducer=new context0_90_10_13();
			Tester<Text, IntWritable, Text, IntWritable> tester=new Tester<Text, IntWritable, Text, IntWritable>();
			IntWritable[] solutionArray = { new IntWritable(1), new IntWritable(2), new IntWritable(3) };
			try {
				tester.test(new Text("key"), solutionArray, reducer);
			} catch (IOException | InterruptedException e1) {
				e1.printStackTrace();
			}
		}
	}

	public void reduce(Text word, Iterable<IntWritable> counts, Context context)
			throws IOException, InterruptedException {
		int wordCount = 0;
		for (IntWritable count : counts) {
			wordCount += count.get();
		}
		context.write(word, new IntWritable(wordCount));
	}  

}
