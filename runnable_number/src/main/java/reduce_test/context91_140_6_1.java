//https://searchcode.com/file/100948405/Mapreduce/Programs/StocksMinMaxOHLC.java#l-11

package reduce_test;

import java.io.IOException;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.Text;

public class context91_140_6_1 implements ReducerC<Text, DoubleWritable>{

	public static void main(String[] args){
		for(int i=0; i<Common.maxcount ; i++){  
			ReducerC<Text, DoubleWritable> reducer=new context91_140_6_1();
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
					double current = value.get();
					max = (max>current)?max:current;
					min = (min<current)?min:current;
				}
				vword.set("Min: " + min + "\tMax: " + max);
				context.write(key, vword);
			}
			

}
