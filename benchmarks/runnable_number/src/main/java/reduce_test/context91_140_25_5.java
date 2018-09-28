//https://searchcode.com/file/71063167/swift-file-system-locality-test/src/main/java/com/mirantis/swift/fs/TestJob.java#l-10

package reduce_test;

import java.io.IOException;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;

public class context91_140_25_5 implements ReducerC<Text, LongWritable>{

	public static void main(String[] args){
		for(int i=0; i<Common.maxcount ; i++){  
			ReducerC<Text, LongWritable> reducer=new context91_140_25_5();
			Tester<Text, LongWritable, Text, LongWritable> tester=new Tester<Text, LongWritable, Text, LongWritable>();
			LongWritable[] solutionArray = { new LongWritable(1), new LongWritable(2), new LongWritable(3) };
			try {
				tester.test(new Text("key"), solutionArray, reducer);
			} catch (IOException | InterruptedException e1) {
				e1.printStackTrace();
			}
		}
	}

	public void reduce(Text key, Iterable<LongWritable> values, Context context) throws IOException, InterruptedException {
		long summ = 0;
		for (LongWritable value : values) {
			summ += value.get();
		}

		context.write(key, new LongWritable(summ));
	}

}
