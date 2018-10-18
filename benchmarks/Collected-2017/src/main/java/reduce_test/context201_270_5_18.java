//https://searchcode.com/file/68443511/hadoop-mapreduce-project/hadoop-mapreduce-examples/src/main/java/org/apache/hadoop/examples/WordMedian.java#l-34

package reduce_test;

import java.io.IOException;
import org.apache.hadoop.io.IntWritable;

public class context201_270_5_18 implements ReducerC<IntWritable, IntWritable>{

	public static void main(String[] args){
		for(int i=0; i<Common.maxcount ; i++){  
			ReducerC<IntWritable, IntWritable> reducer=new context201_270_5_18();
			Tester<IntWritable, IntWritable, IntWritable, IntWritable> tester=new Tester<IntWritable, IntWritable, IntWritable, IntWritable>();
			IntWritable[] solutionArray = { new IntWritable(1), new IntWritable(2), new IntWritable(3) };
			try {
				tester.test(new IntWritable(1), solutionArray, reducer);
			} catch (IOException | InterruptedException e1) {
				e1.printStackTrace();
			}
		}
	}

	private IntWritable val = new IntWritable();

	public void reduce(IntWritable key, Iterable<IntWritable> values,
			Context context) throws IOException, InterruptedException {

		int sum = 0;
		for (IntWritable value : values) {
			sum += value.get();
		}
		val.set(sum);
		context.write(key, val);
	}

}
