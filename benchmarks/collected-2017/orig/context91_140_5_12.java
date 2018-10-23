//https://searchcode.com/file/100948358/Mapreduce/Programs/DistributedCacheExample.java#l-14

package reduce_test;

import java.io.IOException;
import java.util.Map;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;

public class context91_140_5_12 implements ReducerC<Text, LongWritable>{

	public static void main(String[] args){
		for(int i=0; i<Common.maxcount ; i++){  
			ReducerC<Text, LongWritable> reducer=new context91_140_5_12();
			Tester<Text, LongWritable, Text, LongWritable> tester=new Tester<Text, LongWritable, Text, LongWritable>();
			LongWritable[] solutionArray = { new LongWritable(1), new LongWritable(2), new LongWritable(3) };
			try {
				tester.test(new Text("key"), solutionArray, reducer);
			} catch (IOException | InterruptedException e1) {
				e1.printStackTrace();
			}
		}
	}

	Map<String, String> info = null;

	Text kword = new Text();
	LongWritable vword = new LongWritable();
	public void reduce(Text key, Iterable<LongWritable> values, Context context) throws IOException, InterruptedException
	{
		long sum = 0;
		for(LongWritable value : values)
		{
			sum = sum + value.get();
		}
		//String myKey = key.toString() + "\t" + info.get(key.toString());
		//kword.set(myKey);
		vword.set(sum);
		context.write(kword, vword);		
	}

}
