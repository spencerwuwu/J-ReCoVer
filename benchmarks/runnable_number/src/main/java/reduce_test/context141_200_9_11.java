//https://searchcode.com/file/65663873/src/dist/edu/umd/cloud9/example/bigram/BigramCount.java#l-31

package reduce_test;

import java.io.IOException;
import java.util.Iterator;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;

public class context141_200_9_11 implements ReducerC<Text, IntWritable>{

	public static void main(String[] args){
		for(int i=0; i<Common.maxcount ; i++){  
			ReducerC<Text, IntWritable> reducer=new context141_200_9_11();
			Tester<Text, IntWritable, Text, IntWritable> tester=new Tester<Text, IntWritable, Text, IntWritable>();
			IntWritable[] solutionArray = { new IntWritable(1), new IntWritable(2), new IntWritable(3) };
			try {
				tester.test(new Text("key"), solutionArray, reducer);
			} catch (IOException | InterruptedException e1) {
				e1.printStackTrace();
			}
		}
	}

	// Reuse objects
	private final static IntWritable sumWritable = new IntWritable();

	public void reduce(Text key, Iterable<IntWritable> values, Context context)	throws IOException, InterruptedException {
		// sum up values
		int sum = 0;
		Iterator<IntWritable> iter = values.iterator();
		while (iter.hasNext()) {
			sum += iter.next().get();
		}
		sumWritable.set(sum);
		context.write(key, sumWritable);
	}

}
