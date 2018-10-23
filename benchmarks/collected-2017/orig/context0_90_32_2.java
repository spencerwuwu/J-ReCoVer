//https://searchcode.com/file/112450989/src/main/java/ru/sgu/csit/spec/hadoop/pagerank/DoubleSumCombiner.java#l-5

package reduce_test;

import java.io.IOException;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.Text;

public class context0_90_32_2 implements ReducerC<Text, DoubleWritable>{

	public static void main(String[] args){
		for(int i=0; i<Common.maxcount ; i++){  
			ReducerC<Text, DoubleWritable> reducer=new context0_90_32_2();
			Tester<Text, DoubleWritable, Text, DoubleWritable> tester=new Tester<Text, DoubleWritable, Text, DoubleWritable>();
			DoubleWritable[] solutionArray = { new DoubleWritable(1.5), new DoubleWritable(2.5), new DoubleWritable(3.5) };
			try {
				tester.test(new Text("key"), solutionArray, reducer);
			} catch (IOException | InterruptedException e1) {
				e1.printStackTrace();
			}
		}
	}

	private static final double D = 0.85;

	private DoubleWritable result = new DoubleWritable();


	public void reduce(Text key, Iterable<DoubleWritable> values, Context context)
			throws IOException, InterruptedException {

		double sum = 1.0 - D;
		for (DoubleWritable value : values) {
			sum += value.get();
		}
		result.set(sum);

		context.write(key, result);
	}

}
