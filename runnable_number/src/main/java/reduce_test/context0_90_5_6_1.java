//https://searchcode.com/file/116093382/examples/sensors/src/main/java/com/mongodb/hadoop/examples/sensors/LogCombiner.java#l-9

package reduce_test;

import java.io.IOException;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;

public class context0_90_5_6_1 implements ReducerC<Text, IntWritable>{

	public static void main(String[] args){
		for(int i=0; i<Common.maxcount ; i++){  
			ReducerC<Text, IntWritable> reducer=new context0_90_5_6_1();
			Tester<Text, IntWritable, Text, IntWritable> tester=new Tester<Text, IntWritable, Text, IntWritable>();
			IntWritable[] solutionArray = { new IntWritable(1), new IntWritable(2), new IntWritable(3) };
			try {
				tester.test(new Text("key"), solutionArray, reducer);
			} catch (IOException | InterruptedException e1) {
				e1.printStackTrace();
			}
		}
	}

	public void reduce(final Text pKey, final Iterable<IntWritable> pValues, final Context pContext)
			throws IOException, InterruptedException {

		int count = 0;
		for (IntWritable val : pValues) {
			count += val.get();
		}

		pContext.write(pKey, new IntWritable(count));
	}

}
