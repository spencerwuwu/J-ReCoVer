//https://searchcode.com/file/8195359/hadoop-mapreduce-project/hadoop-mapreduce-client/hadoop-mapreduce-client-core/src/main/java/org/apache/hadoop/mapreduce/lib/reduce/IntSumReducer.java#l-19

package reduce_test;

import java.io.IOException;
import org.apache.hadoop.io.IntWritable;

public class context20_0_90_11_15 implements ReducerC<IntWritable,IntWritable>{

	public static void main(String[] args){
		for(int i=0; i<Common.maxcount ; i++){  
			ReducerC<IntWritable,IntWritable> reducer=new context20_0_90_11_15();
			Tester<IntWritable,IntWritable, IntWritable,IntWritable> tester=new Tester<IntWritable,IntWritable, IntWritable,IntWritable>();
			IntWritable[] solutionArray = { new IntWritable(1), new IntWritable(2), new IntWritable(3) };
			try {
				tester.test(new IntWritable(1), solutionArray, reducer);
			} catch (IOException | InterruptedException e1) {
				e1.printStackTrace();
			}
		}
	}

	private IntWritable result = new IntWritable();

	public void reduce(IntWritable key, Iterable<IntWritable> values, 
			Context context) throws IOException, InterruptedException {
		int sum = 0;
		for (IntWritable val : values) {
			sum += val.get();
		}
		result.set(sum);
		context.write(key, result);
	}

}

