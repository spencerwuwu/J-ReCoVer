//https://searchcode.com/file/100948353/Mapreduce/Programs/CalculateAverageMovieRating.java#l-9

package reduce_test;

import java.io.IOException;
import org.apache.hadoop.io.FloatWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;

public class context0_90_4_7 implements ReducerC<Text, LongWritable>{

	public static void main(String[] args){
		for(int i=0; i<Common.maxcount ; i++){  
			ReducerC<Text, LongWritable> reducer=new context0_90_4_7();
			Tester<Text, LongWritable, Text, FloatWritable> tester=new Tester<Text, LongWritable, Text, FloatWritable>();
			LongWritable[] solutionArray = { new LongWritable(1), new LongWritable(2), new LongWritable(3) };
			try {
				tester.test(new Text("key"), solutionArray, reducer);
			} catch (IOException | InterruptedException e1) {
				e1.printStackTrace();
			}
		}
	}

	FloatWritable vword = new FloatWritable();
	public void reduce(Text key, Iterable<LongWritable> values, Context context) throws IOException, InterruptedException
	{
		float ratingAvg = 0.0f;
		long sum = 0;
		int counter = 0;
		for(LongWritable value : values)
		{
			counter++;
			sum = sum + value.get();
		}
		ratingAvg = (float) (sum / counter);
		vword.set(ratingAvg);
		context.write(key, vword);
	}

}
