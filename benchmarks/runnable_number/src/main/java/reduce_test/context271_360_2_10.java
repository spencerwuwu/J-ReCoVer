//https://searchcode.com/file/70647403/MRDP/src/main/java/mrdp/ch6/JobChainingDriver.java#l-24

package reduce_test;

import java.io.IOException;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;

public class context271_360_2_10 implements ReducerC<Text, LongWritable>{

	public static void main(String[] args){
		for(int i=0; i<Common.maxcount ; i++){  
			ReducerC<Text, LongWritable> reducer=new context271_360_2_10();
			Tester<Text, LongWritable, Text, LongWritable> tester=new Tester<Text, LongWritable, Text, LongWritable>();
			LongWritable[] solutionArray = { new LongWritable(1), new LongWritable(2), new LongWritable(3) };
			try {
				tester.test(new Text("key"), solutionArray, reducer);
			} catch (IOException | InterruptedException e1) {
				e1.printStackTrace();
			}
		}
	}

	public static final String USERS_COUNTER_NAME = "Users";
	private LongWritable outvalue = new LongWritable();

	public void reduce(Text key, Iterable<LongWritable> values,
			Context context) throws IOException, InterruptedException {

		// Increment user counter, as each reduce group represents one user
		//context.getCounter(AVERAGE_CALC_GROUP, USERS_COUNTER_NAME).increment(1);

		int sum = 0;

		for (LongWritable value : values) {
			sum += value.get();
		}

		outvalue.set(sum);
		context.write(key, outvalue);
	}

}
