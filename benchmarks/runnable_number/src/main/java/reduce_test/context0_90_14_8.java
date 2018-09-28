//https://searchcode.com/file/65735686/src/main/java/com/nearinfinity/hadoop/patent/CitationHistogram.java#l-14

package reduce_test;

import java.io.IOException;
import org.apache.hadoop.io.IntWritable;

public class context0_90_14_8 implements ReducerC<IntWritable, IntWritable>{

	public static void main(String[] args){
		for(int i=0; i<Common.maxcount ; i++){  
			ReducerC<IntWritable, IntWritable> reducer=new context0_90_14_8();
			Tester<IntWritable, IntWritable, IntWritable, IntWritable> tester=new Tester<IntWritable, IntWritable, IntWritable, IntWritable>();
			IntWritable[] solutionArray = { new IntWritable(1), new IntWritable(2), new IntWritable(3) };
			try {
				tester.test(new IntWritable(1), solutionArray, reducer);
			} catch (IOException | InterruptedException e1) {
				e1.printStackTrace();
			}
		}
	}

	private IntWritable frequency = new IntWritable();

	public void reduce(IntWritable key, Iterable<IntWritable> values, Context context)
			throws IOException, InterruptedException {

		int count = 0;
		for (IntWritable value : values) {
			count += value.get();
		}
		frequency.set(count);
		context.write(key, frequency);
		//       context.getCounter(Counters.TOTAL_PATENTS).increment(1L);
	}

}