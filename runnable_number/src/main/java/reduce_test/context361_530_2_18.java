//https://searchcode.com/file/77165600/src/test/mapred/org/apache/hadoop/mapreduce/lib/join/TestJoinDatamerge.java#l-35

package reduce_test;

import java.io.IOException;
import org.apache.hadoop.io.IntWritable;

public class context361_530_2_18 implements ReducerC<IntWritable, IntWritable>{

	public static void main(String[] args){
		for(int i=0; i<Common.maxcount ; i++){  
			ReducerC<IntWritable, IntWritable> reducer=new context361_530_2_18();
			Tester<IntWritable, IntWritable, IntWritable, IntWritable> tester=new Tester<IntWritable, IntWritable, IntWritable, IntWritable>();
			IntWritable[] solutionArray = { new IntWritable(1), new IntWritable(2), new IntWritable(3) };
			try {
				tester.test(new IntWritable(1), solutionArray, reducer);
			} catch (IOException | InterruptedException e1) {
				e1.printStackTrace();
			}
		}
	}

	public void reduce(IntWritable key, Iterable<IntWritable> values,
			Context context) throws IOException, InterruptedException {
		int seen = 0;
		for (IntWritable value : values) {
			seen += value.get();
		}
		//assertTrue("Bad count for " + key.get(), verify(key.get(), seen));
		context.write(key, new IntWritable(seen));
	}


}
