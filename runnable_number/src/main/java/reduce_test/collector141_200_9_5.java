//https://searchcode.com/file/65662303/src/dist/edu/umd/cloud9/collection/medline/NumberMedlineCitations.java#l-32

package reduce_test;

import java.io.IOException;
import java.util.Iterator;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.mapred.Reporter;

public class collector141_200_9_5 implements ReducerO<IntWritable, IntWritable, IntWritable, IntWritable> {

	public static void main(String[] args){
		for(int i=0; i<Common.maxcount ; i++){  
			ReducerO<IntWritable, IntWritable, IntWritable, IntWritable> reducer=new collector141_200_9_5();
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

	public void reduce(IntWritable key, Iterator<IntWritable> values,
			OutputCollector<IntWritable, IntWritable> output, Reporter reporter) throws IOException {
		output.collect(key, cnt);
		cnt.set(cnt.get() + 1);
	}

}