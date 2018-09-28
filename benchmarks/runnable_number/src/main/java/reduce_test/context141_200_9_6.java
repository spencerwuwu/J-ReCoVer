//https://searchcode.com/file/65662332/src/dist/edu/umd/cloud9/collection/medline/NumberMedlineCitations2.java#l-27

package reduce_test;

import java.io.IOException;
import org.apache.hadoop.io.IntWritable;

public class context141_200_9_6 implements ReducerC<IntWritable, IntWritable>{

	public static void main(String[] args){
		for(int i=0; i<Common.maxcount ; i++){  
			ReducerC<IntWritable, IntWritable> reducer=new context141_200_9_6();
			Tester<IntWritable, IntWritable, IntWritable, IntWritable> tester=new Tester<IntWritable, IntWritable, IntWritable, IntWritable>();
			IntWritable[] solutionArray = { new IntWritable(1), new IntWritable(2), new IntWritable(3) };
			try {
				tester.test(new IntWritable(1), solutionArray, reducer);
			} catch (IOException | InterruptedException e1) {
				e1.printStackTrace();
			}
		}
	}

	private final static IntWritable cnt = new IntWritable(1);

	public void reduce(IntWritable key, Iterable<IntWritable> values, Context context)
			throws IOException, InterruptedException {
		context.write(key, cnt);
		cnt.set(cnt.get() + 1);
	}


}
