//https://searchcode.com/file/99975914/hw2/Li_Lullo_Martin_exercise2.java#l-16

package reduce_test;

import java.io.IOException;
import java.util.Iterator;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.Reporter;

public class collector91_140_1_11 implements ReducerO<Text, DoubleWritable, Text,DoubleWritable> {

	public static void main(String[] args){
		for(int i=0; i<Common.maxcount ; i++){  
			ReducerO<Text, DoubleWritable, Text,DoubleWritable> reducer=new collector91_140_1_11();
			Tester<Text, DoubleWritable, Text,DoubleWritable> tester=new Tester<Text, DoubleWritable, Text,DoubleWritable>();
			DoubleWritable[] solutionArray = { new DoubleWritable(1.5), new DoubleWritable(2.5), new DoubleWritable(3.5) };
			try {
				tester.test(new Text("key"), solutionArray, reducer);
			} catch (IOException | InterruptedException e1) {
				e1.printStackTrace();
			}
		}
	} 

	public void reduce(Text key, Iterator<DoubleWritable> values, OutputCollector<Text, DoubleWritable> output, Reporter reporter) throws IOException {
		double stdDev = 0;
		double sumSqr = 0;
		double count = 0;
		double mean = 0;
		double sum = 0;
		while(values.hasNext()){
			double value = values.next().get();
			sumSqr += value*value;
			sum += value;
			count++;
		}
		mean = sum/count;
		stdDev = Math.sqrt((sumSqr-count*mean*mean)/count);
		output.collect(key, new DoubleWritable(stdDev));
	}

}