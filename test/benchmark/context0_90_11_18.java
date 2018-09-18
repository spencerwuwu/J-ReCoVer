//https://searchcode.com/file/58018978/web-crawler/src/main/java/org/profile/mapreduce/EMRReducer.java#l-7

package reduce_test;

import java.io.IOException;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;

public class context0_90_11_18 implements ReducerC<Text, IntWritable>{

	public static void main(String[] args){
		for(int i=0; i<Common.maxcount ; i++){  
			ReducerC<Text, IntWritable> reducer=new context0_90_11_18();
			Tester<Text, IntWritable, Text, Text> tester=new Tester<Text, IntWritable, Text, Text>();
			IntWritable[] solutionArray = { new IntWritable(1), new IntWritable(2), new IntWritable(3) };
			try {
				tester.test(new Text("key"), solutionArray, reducer);
			} catch (IOException | InterruptedException e1) {
				e1.printStackTrace();
			}
		}
	}

	public void reduce(Text key, Iterable<IntWritable> values, Context context) throws IOException,
	InterruptedException {
		int sum = 0;
		for (IntWritable value : values) {
			sum += value.get();
		}
		context.write(key, new Text(sum + ""));
		//context.getCounter(EMRDriver.STATE_COUNTER_GROUP, EMRDriver.TOTAL_PROFILE_COUNT).increment(1);

	}

}
