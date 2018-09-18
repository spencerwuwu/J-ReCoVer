//https://searchcode.com/file/94159078/src/main/java/GenomeMapReduce.java#l-24

package reduce_test;

import java.io.IOException;
import java.util.Iterator;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.mapred.Reporter;

public class collector0_90_1_7 implements ReducerO<IntWritable, IntWritable, IntWritable, IntWritable> {

	public static void main(String[] args){
		for(int i=0; i < Common.maxcount; i++){  
			ReducerO<IntWritable, IntWritable, IntWritable, IntWritable> reducer=new collector0_90_1_7();
			Tester<IntWritable, IntWritable, IntWritable, IntWritable> tester=new Tester<IntWritable, IntWritable, IntWritable, IntWritable>();
			IntWritable[] solutionArray = { new IntWritable(1), new IntWritable(2), new IntWritable(3) };
			try {
				tester.test(new IntWritable(1), solutionArray, reducer);
			} catch (IOException | InterruptedException e1) {
				e1.printStackTrace();
			}
		}
	}

	public void reduce(IntWritable key, Iterator<IntWritable> values, OutputCollector<IntWritable, IntWritable> output, Reporter reporter) throws IOException {
		int count = 0;
		while (values.hasNext()) count += values.next().get();
		output.collect(key, new IntWritable(count));
	}

}
