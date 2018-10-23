//https://searchcode.com/file/93068483/src/mapred/org/apache/hadoop/mapreduce/Reducer.java#l-19

package reduce_test;

import java.io.IOException;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;

public class context39_141_200_1_1_2 implements ReducerC<Text,IntWritable>{

	public static void main(String[] args){
		for(int i=0; i<Common.maxcount ; i++){  
			ReducerC<Text,IntWritable> reducer=new context39_141_200_1_1_2();
			Tester<Text,IntWritable, Text,IntWritable> tester=new Tester<Text,IntWritable, Text,IntWritable>();
			IntWritable[] solutionArray = { new IntWritable(1), new IntWritable(2), new IntWritable(3) };
			try {
				tester.test(new Text("key"), solutionArray, reducer);
			} catch (IOException | InterruptedException e1) {
				e1.printStackTrace();
			}
		}
	}

	public void reduce(Text key, Iterable<IntWritable> values, Context context
			) throws IOException, InterruptedException {
		for(IntWritable value: values) {
			context.write((Text) key, (IntWritable) value);
		}
	}

}
