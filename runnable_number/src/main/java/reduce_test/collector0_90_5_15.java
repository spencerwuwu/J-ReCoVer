//https://searchcode.com/file/99757467/modules/hadoop/src/test/java/org/gridgain/grid/kernal/processors/hadoop/examples/GridHadoopWordCount1Reduce.java#l-35

package reduce_test;

import java.io.IOException;
import java.util.Iterator;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.Reporter;

public class collector0_90_5_15 implements ReducerO<Text, IntWritable, Text, IntWritable> {

	public static void main(String[] args){
		for(int i=0; i<Common.maxcount ; i++){  
			ReducerO<Text, IntWritable, Text, IntWritable> reducer=new collector0_90_5_15();
			Tester<Text, IntWritable, Text, IntWritable> tester=new Tester<Text, IntWritable, Text, IntWritable>();
			IntWritable[] solutionArray = { new IntWritable(1), new IntWritable(2), new IntWritable(3) };
			try {
				tester.test(new Text("key"), solutionArray, reducer);
			} catch (IOException | InterruptedException e1) {
				e1.printStackTrace();
			}
		}
	} 

	private boolean wasConfigured;

	public void reduce(Text key, Iterator<IntWritable> values, OutputCollector<Text, IntWritable> output, Reporter reporter)
			throws IOException {
		assert wasConfigured : "Reducer should be configured";

	int sum = 0;

	while (values.hasNext())
		sum += values.next().get();

	output.collect(key, new IntWritable(sum));
	}

}
