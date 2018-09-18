//https://searchcode.com/file/69347180/core/src/main/java/org/apache/mahout/classifier/bayes/common/BayesFeatureReducer.java#l-21

package reduce_test;

import java.io.IOException;
import java.util.Iterator;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.Reporter;

public class collector0_90_10_5 implements ReducerO<Text, DoubleWritable, Text,DoubleWritable> {

	public static void main(String[] args){
		for(int i=0; i<Common.maxcount ; i++){  
			ReducerO<Text, DoubleWritable, Text,DoubleWritable> reducer=new collector0_90_10_5();
			Tester<Text, DoubleWritable, Text,DoubleWritable> tester=new Tester<Text, DoubleWritable, Text,DoubleWritable>();
			DoubleWritable[] solutionArray = { new DoubleWritable(1.5), new DoubleWritable(2.5), new DoubleWritable(3.5) };
			try {
				tester.test(new Text("key"), solutionArray, reducer);
			} catch (IOException | InterruptedException e1) {
				e1.printStackTrace();
			}
		}
	} 

	public void reduce(Text key,
			Iterator<DoubleWritable> values,
			OutputCollector<Text, DoubleWritable> output,
			Reporter reporter) throws IOException {
		//Key is label,word, value is the number of times we've seen this label word per local node.  Output is the same

		double sum = 0.0;
		while (values.hasNext()) {
			sum += values.next().get();
		}
		output.collect(key, new DoubleWritable(sum));
	}

}