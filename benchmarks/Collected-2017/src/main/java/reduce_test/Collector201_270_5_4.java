//https://searchcode.com/file/70647392/MRDP/src/main/java/mrdp/ch6/ChainMapperDriver.java#l-44

package reduce_test;

import java.io.IOException;
import java.util.Iterator;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.Reporter;

public class Collector201_270_5_4 implements ReducerO<Text, LongWritable, Text, LongWritable>{

	public static void main(String[] args){
		for(int i=0; i<Common.maxcount ; i++){  
			ReducerO<Text, LongWritable, Text, LongWritable> reducer=new Collector201_270_5_4();
			Tester<Text, LongWritable, Text, LongWritable> tester=new Tester<Text, LongWritable, Text, LongWritable>();
			LongWritable[] solutionArray = { new LongWritable(1), new LongWritable(2), new LongWritable(3) };
			try {
				tester.test(new Text("key"), solutionArray, reducer);
			} catch (IOException | InterruptedException e1) {
				e1.printStackTrace();
			}
		}
	}

	private LongWritable outvalue = new LongWritable();

	public void reduce(Text key, Iterator<LongWritable> values,
			OutputCollector<Text, LongWritable> output, Reporter reporter)
					throws IOException {

		int sum = 0;
		while (values.hasNext()) {
			sum += values.next().get();
		}
		outvalue.set(sum);
		output.collect(key, outvalue);
	}

}
