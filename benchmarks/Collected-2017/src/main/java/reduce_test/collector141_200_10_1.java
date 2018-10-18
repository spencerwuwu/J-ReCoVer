//https://searchcode.com/file/73390634/src/Corrector/IdentifyTrustedReads.java#l-29

package reduce_test;

import java.io.IOException;
import java.util.Iterator;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.Reporter;

public class collector141_200_10_1 implements ReducerO<Text, IntWritable, Text, IntWritable> {

	public static void main(String[] args){
		for(int i=0; i<Common.maxcount ; i++){  
			ReducerO<Text, IntWritable, Text, IntWritable> reducer=new collector141_200_10_1();
			Tester<Text, IntWritable, Text, IntWritable> tester=new Tester<Text, IntWritable, Text, IntWritable>();
			IntWritable[] solutionArray = { new IntWritable(1), new IntWritable(2), new IntWritable(3) };
			try {
				tester.test(new Text("key"), solutionArray, reducer);
			} catch (IOException | InterruptedException e1) {
				e1.printStackTrace();
			}
		}
	}  

	private static long KmerThreshold = 1 ;

	public void reduce(Text prefix, Iterator<IntWritable> iter,
			OutputCollector<Text, IntWritable> output, Reporter reporter)
					throws IOException
	{
		int sum =0;
		int untrust_count = 0;
		int TRUST = 1;
		while(iter.hasNext())
		{
			int frequency = iter.next().get();
			if (frequency <= KmerThreshold) {
				untrust_count = untrust_count + 1;
				TRUST = 0;
				break;
			}
		}

		output.collect(prefix, new IntWritable(TRUST));
	}

}