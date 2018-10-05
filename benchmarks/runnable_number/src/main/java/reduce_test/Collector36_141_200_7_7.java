//https://searchcode.com/file/110287814/src/test/mapred/org/apache/hadoop/mapred/TestJavaSerialization.java#l-53

package reduce_test;

import java.io.IOException;
import java.util.Iterator;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.mapred.Reporter;

public class Collector36_141_200_7_7 implements ReducerO<LongWritable, Long, LongWritable, Long> {

	public static void main(String[] args){
		for(int i=0; i<Common.maxcount ; i++){  
			ReducerO<LongWritable, Long, LongWritable, Long> reducer=new Collector36_141_200_7_7();
			Tester<LongWritable, Long, LongWritable, Long> tester=new Tester<LongWritable, Long, LongWritable, Long>();
			Long[] solutionArray = { 1L, 2L, 3L, 4L, 5L, 6L, 16L, 15L, 14L, 13L, 12L, 11L };
			try {
				tester.test(new LongWritable(1), solutionArray, reducer);
			} catch (IOException | InterruptedException e1) {
				e1.printStackTrace();
			}
		}
	} 

	public void reduce(LongWritable key, Iterator<Long> values,
	        OutputCollector<LongWritable, Long> output, Reporter reporter)
	      throws IOException {

	      long sum = 0;
	      while (values.hasNext()) {
	        sum += values.next();
	      }
	      output.collect(key, sum);
	    }
}