//https://searchcode.com/file/100948390/Mapreduce/Programs/SequenceMapReduce.java#l-8

package reduce_test;

import java.io.IOException;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;

public class context0_90_4_10 implements ReducerC<Text, LongWritable>{

	public static void main(String[] args){
		for(int i=0; i<Common.maxcount ; i++){  
			ReducerC<Text, LongWritable> reducer=new context0_90_4_10();
			Tester<Text, LongWritable, Text, LongWritable> tester=new Tester<Text, LongWritable, Text, LongWritable>();
			LongWritable[] solutionArray = { new LongWritable(1), new LongWritable(2), new LongWritable(3) };
			try {
				tester.test(new Text("key"), solutionArray, reducer);
			} catch (IOException | InterruptedException e1) {
				e1.printStackTrace();
			}
		}
	}

	  LongWritable vword = new LongWritable();
		public void reduce(Text key, Iterable<LongWritable> values, Context context) throws IOException, InterruptedException
		{
			long sum = 0;
			for(LongWritable value : values)
			{
				sum = sum + value.get();
			}
			vword.set(sum);
			context.write(key, vword);
		}

}
