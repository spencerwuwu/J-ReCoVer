//https://searchcode.com/file/65662211/src/dist/edu/umd/cloud9/collection/line/NumberTextDocuments.java#l-33

package reduce_test;

import java.io.IOException;
import java.util.Iterator;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.Reporter;

public class collector141_200_9_4 implements ReducerO<Text, IntWritable, Text, IntWritable> {

	public static void main(String[] args){
		for(int i=0; i<Common.maxcount ; i++){  
			ReducerO<Text, IntWritable, Text, IntWritable> reducer=new collector141_200_9_4();
			Tester<Text, IntWritable, Text, IntWritable> tester=new Tester<Text, IntWritable, Text, IntWritable>();
			IntWritable[] solutionArray = { new IntWritable(1), new IntWritable(2), new IntWritable(3) };
			try {
				tester.test(new Text("key"), solutionArray, reducer);
			} catch (IOException | InterruptedException e1) {
				e1.printStackTrace();
			}
		}
	}  

	private final static IntWritable sCnt = new IntWritable(1);

	public void reduce(Text key, Iterator<IntWritable> values,
			OutputCollector<Text, IntWritable> output, Reporter reporter) throws IOException {
		output.collect(key, sCnt);
		sCnt.set(sCnt.get() + 1);
	}

}