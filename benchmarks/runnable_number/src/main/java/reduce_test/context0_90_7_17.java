//https://searchcode.com/file/98860514/CombinerMR/WordCountCombiner.java#l-14

package reduce_test;

import java.io.IOException;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;

public class context0_90_7_17 implements ReducerC<Text, IntWritable>{

	public static void main(String[] args){
		for(int i=0; i<Common.maxcount ; i++){  
			ReducerC<Text, IntWritable> reducer=new context0_90_7_17();
			Tester<Text, IntWritable, Text, IntWritable> tester=new Tester<Text, IntWritable, Text, IntWritable>();
			IntWritable[] solutionArray = { new IntWritable(1), new IntWritable(2), new IntWritable(3) };
			try {
				tester.test(new Text("key"), solutionArray, reducer);
			} catch (IOException | InterruptedException e1) {
				e1.printStackTrace();
			}
		}
	}

	private IntWritable result = new IntWritable();

	public void reduce(Text key, Iterable<IntWritable> values, 
			Context context
			) throws IOException, InterruptedException {
		// since the mapper output and reducer outputs are of same type we cna keep the key value types same
		// we will filter keys and eleminate any records that has word Berners-Lee or whos total vlaues are less then min value

		int sum = 0;
		for (IntWritable val : values) {
			sum += val.get();
		}
		result.set(sum);
		// access runtime parameter for min value
		//Configuration conf = context.getConfiguration();
		int min=0;
		// filter for a value, 
		if((key.toString().equals("Berners-Lee")==false) || sum>min ) context.write(key, result);
	}

}
