//https://searchcode.com/file/100948347/Mapreduce/Programs/StockGlobalSort.java#l-20

package reduce_test;

import java.io.IOException;
import java.util.Iterator;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.Reporter;

public class Collector91_140_5_11 implements ReducerO<Text, LongWritable, Text, LongWritable> {

	public static void main(String[] args){
		for(int i=0; i<Common.maxcount ; i++){  
			ReducerO<Text, LongWritable, Text, LongWritable> reducer=new Collector91_140_5_11();
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
	public void reduce(Text key, Iterator<LongWritable> values,
			OutputCollector<Text, LongWritable> collector, Reporter reporter)
					throws IOException {
		long sum = 0L;
		while(values.hasNext())
		{
			sum = sum + values.next().get();
		}
		vword.set(sum);
		collector.collect(key, vword);
	}
}