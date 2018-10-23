//https://searchcode.com/file/73347357/src/edu/upenn/mkse212/pennbook/hadoop/DiffSortReducer.java#l-17

package reduce_test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.IntWritable;

public class context0_90_11_2 implements ReducerC<IntWritable, DoubleWritable>{

	public static void main(String[] args){
		for(int i=0; i<Common.maxcount ; i++){  
			ReducerC<IntWritable, DoubleWritable> reducer=new context0_90_11_2();
			Tester<IntWritable, DoubleWritable, IntWritable, DoubleWritable> tester=new Tester<IntWritable, DoubleWritable, IntWritable, DoubleWritable>();
			DoubleWritable[] solutionArray = { new DoubleWritable(1.5), new DoubleWritable(2.5), new DoubleWritable(3.5) };
			try {
				tester.test(new IntWritable(1), solutionArray, reducer);
			} catch (IOException | InterruptedException e1) {
				e1.printStackTrace();
			}
		}
	}

	public static final double D = 0.15;
	public void reduce(IntWritable key, Iterable<DoubleWritable> values, Context context) throws IOException, InterruptedException 
	{
		ArrayList<Double> diffs = new ArrayList<Double>();//list of differences computed
		for (DoubleWritable diffValue : values)
		{
			diffs.add(diffValue.get());
		}

		Collections.sort(diffs);
		double maxDiff = diffs.get(diffs.size()-1);//get last value (largest diff)

		context.write(null, new DoubleWritable(maxDiff));
	}

}
