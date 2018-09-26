//https://searchcode.com/file/94159083/src/main/java/WordCount.java#l-22

package reduce_test;

import java.io.IOException;
import java.util.Iterator;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.Reporter;

public class collector91_140_1_7 implements ReducerO<Text, IntWritable, Text, IntWritable> {

	public static void main(String[] args){
		for(int i=0; i<Common.maxcount ; i++){  
			ReducerO<Text, IntWritable, Text, IntWritable> reducer=new collector91_140_1_7();
			Tester<Text, IntWritable, Text, IntWritable> tester=new Tester<Text, IntWritable, Text, IntWritable>();
			IntWritable[] solutionArray = { new IntWritable(1), new IntWritable(2), new IntWritable(3) };
			try {
				tester.test(new Text("key"), solutionArray, reducer);
			} catch (IOException | InterruptedException e1) {
				e1.printStackTrace();
			}
		}
	} 

	public void reduce( Text key, Iterator<IntWritable> values,
			OutputCollector<Text, IntWritable> output,
			Reporter reporter) throws IOException
	{
		// Iterate over all of the values (counts of occurrences of this word)
		int count = 0;
		while( values.hasNext() )
		{
			// Add the value to our count
			count += values.next().get();
		}

		// Output the word with its count (wrapped in an IntWritable)
		output.collect( key, new IntWritable( count ) );
	}

}
