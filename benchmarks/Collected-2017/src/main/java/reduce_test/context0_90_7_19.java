//https://searchcode.com/file/98860610/lession9/RecordlkpReduce.java#l-12

package reduce_test;

import java.io.IOException;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;

public class context0_90_7_19 implements ReducerC<Text, IntWritable>{

	public static void main(String[] args){
		for(int i=0; i<Common.maxcount ; i++){  
			ReducerC<Text, IntWritable> reducer=new context0_90_7_19();
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
			Context context
			) throws IOException, InterruptedException {
		System.out.println("Reducers Sort and Shuffle Merge Brings keys Over HTTP"+ key +" and there  Values  our case its <key> <v1> <v1> each being 1 " );

		for (IntWritable value : values)
		{
			context.write(key, value);
		}
	}

}
