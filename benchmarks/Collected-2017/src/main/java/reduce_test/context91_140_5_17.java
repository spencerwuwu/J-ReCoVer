//https://searchcode.com/file/100948386/Mapreduce/Programs/StockVolumeAvgSum.java#l-10

package reduce_test;

import java.io.IOException;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;

public class context91_140_5_17 implements ReducerC<Text, LongWritable>{

	public static void main(String[] args){
		for(int i=0; i<Common.maxcount ; i++){  
			ReducerC<Text, LongWritable> reducer=new context91_140_5_17();
			Tester<Text, LongWritable, Text, LongWritable> tester=new Tester<Text, LongWritable, Text, LongWritable>();
			LongWritable[] solutionArray = { new LongWritable(1), new LongWritable(2), new LongWritable(3) };
			try {
				tester.test(new Text("key"), solutionArray, reducer);
			} catch (IOException | InterruptedException e1) {
				e1.printStackTrace();
			}
		}
	}

	Text vword = new Text();
	public void reduce(Text key, Iterable<LongWritable> values, Context context) throws IOException, InterruptedException
	{
		long sum = 0;
		double avg = 0.0;
		int counter = 0;
		for(LongWritable value : values)
		{
			sum = sum + value.get();
			counter++;
		}
		avg = (double) sum / counter;
		vword.set("sum: " + sum + "\tAverage: " + avg);
		context.write(key, vword);
	}

}
