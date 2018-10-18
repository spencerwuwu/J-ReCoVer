//https://searchcode.com/file/100948365/Mapreduce/Programs/StockMinMaxReducer.java#l-6

package reduce_test;

import java.io.IOException;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.Text;

public class context0_90_8_2 implements ReducerC<Text, DoubleWritable>{

	public static void main(String[] args){
		for(int i=0; i<Common.maxcount ; i++){  
			ReducerC<Text, DoubleWritable> reducer=new context0_90_8_2();
			Tester<Text, DoubleWritable, Text, Text> tester=new Tester<Text, DoubleWritable, Text, Text>();
			DoubleWritable[] solutionArray = { new DoubleWritable(1.5), new DoubleWritable(2.5), new DoubleWritable(3.5) };
			try {
				tester.test(new Text("key"), solutionArray, reducer);
			} catch (IOException | InterruptedException e1) {
				e1.printStackTrace();
			}
		}
	}

	Text vword = new Text();
	public void reduce(Text key, Iterable<DoubleWritable> values, Context context) throws IOException, InterruptedException
	{
		double min = Double.MAX_VALUE;
		double max = 0.0;
		for(DoubleWritable value : values)
		{
			double myvalue = value.get();
			max = (max > myvalue) ? max : myvalue;
			min = (min < myvalue) ? min : myvalue;
		}
		String minmax = "Min is " + min + ", Max is " + max;
		vword.set(minmax);
		context.write(key, vword);
	}

}