//https://searchcode.com/file/70647354/MRDP/src/main/java/mrdp/ch6/ParallelJobs.java#l-11

package reduce_test;

import java.io.IOException;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.Text;

public class context91_140_25_3 implements ReducerC<Text, DoubleWritable>{

	public static void main(String[] args){
		for(int i=0; i<Common.maxcount ; i++){  
			ReducerC<Text, DoubleWritable> reducer=new context91_140_25_3();
			Tester<Text, DoubleWritable, Text, DoubleWritable> tester=new Tester<Text, DoubleWritable, Text, DoubleWritable>();
			DoubleWritable[] solutionArray = { new DoubleWritable(1.5), new DoubleWritable(2.5), new DoubleWritable(3.5) };
			try {
				tester.test(new Text("key"), solutionArray, reducer);
			} catch (IOException | InterruptedException e1) {
				e1.printStackTrace();
			}
		}
	}

	private DoubleWritable outvalue = new DoubleWritable();


	public void reduce(Text key, Iterable<DoubleWritable> values,
			Context context) throws IOException, InterruptedException {

		double sum = 0.0;
		double count = 0;
		for (DoubleWritable dw : values) {
			sum += dw.get();
			++count;
		}

		outvalue.set(sum / count);
		context.write(key, outvalue);
	}

}
