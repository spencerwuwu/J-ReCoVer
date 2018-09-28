//https://searchcode.com/file/99975919/hw2/Li_Lullo_Martin_exercise1.java#l-20

package reduce_test;

import java.io.IOException;
import java.util.Iterator;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.Reporter;

public class collector141_200_1_16 implements ReducerO<Text, DoubleWritable, Text,DoubleWritable> {

	public static void main(String[] args){
		for(int i=0; i<Common.maxcount ; i++){  
			ReducerO<Text, DoubleWritable, Text,DoubleWritable> reducer=new collector141_200_1_16();
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
		double avgVol = 0;
		double sum = 0;
		double count = 0;
		while(values.hasNext()){
			sum += values.next().get();
			count++;
		}
		avgVol = sum/count;
		output.collect(key, new DoubleWritable(avgVol));
	}

}