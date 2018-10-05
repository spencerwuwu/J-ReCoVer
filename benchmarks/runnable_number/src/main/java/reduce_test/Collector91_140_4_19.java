//https://searchcode.com/file/100948350/Mapreduce/Programs/MyChainMapper.java#l-65

package reduce_test;

import java.io.IOException;
import java.util.Iterator;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.Reporter;

public class Collector91_140_4_19 implements ReducerO<Text, LongWritable, Text, LongWritable> {

	public static void main(String[] args){
		for(int i=0; i<Common.maxcount ; i++){  
			ReducerO<Text, LongWritable, Text, LongWritable> reducer=new Collector91_140_4_19();
			Tester<Text, LongWritable, Text, LongWritable> tester=new Tester<Text, LongWritable, Text, LongWritable>();
			LongWritable[] solutionArray = { new LongWritable(1), new LongWritable(2), new LongWritable(3) };
			try {
				tester.test(new Text("key"), solutionArray, reducer);
			} catch (IOException | InterruptedException e1) {
				e1.printStackTrace();
			}
		}
	}

	public void reduce(Text key, Iterator<LongWritable> values,
			OutputCollector<Text, LongWritable> collect, Reporter reporter)
					throws IOException {
		long sum = 0;
		while(values.hasNext())
		{
			sum = sum + values.next().get();
		}
		collect.collect(key, new LongWritable(sum));
	}
}
