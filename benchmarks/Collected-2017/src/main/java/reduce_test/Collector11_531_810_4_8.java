//https://searchcode.com/file/10576200/hadoop-mapreduce-project/hadoop-mapreduce-client/hadoop-mapreduce-client-jobclient/src/test/java/org/apache/hadoop/mapred/GenericMRLoadGenerator.java#l-42

package reduce_test;

import java.io.IOException;
import java.util.Iterator;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.mapred.Reporter;

public class Collector11_531_810_4_8 implements ReducerO<LongWritable, LongWritable, LongWritable, LongWritable> {

	public static void main(String[] args){
		for(int i=0; i<Common.maxcount ; i++){  
			ReducerO<LongWritable, LongWritable, LongWritable, LongWritable> reducer=new Collector11_531_810_4_8();
			Tester<LongWritable, LongWritable, LongWritable, LongWritable> tester=new Tester<LongWritable, LongWritable, LongWritable, LongWritable>();
			LongWritable[] solutionArray = { new LongWritable(1), new LongWritable(2), new LongWritable(3) };
			try {
				tester.test(new LongWritable(1), solutionArray, reducer);
			} catch (IOException | InterruptedException e1) {
				e1.printStackTrace();
			}
		}
	} 

	private long total;
	private long kept = 0;
	private float keep;


	protected void emit(LongWritable key, LongWritable val, OutputCollector<LongWritable,LongWritable> out)
			throws IOException {
		++total;
		while((float) kept / total < keep) {
			++kept;
			out.collect(key, val);
		}
	}

	public void reduce(LongWritable key, Iterator<LongWritable> values,
			OutputCollector<LongWritable,LongWritable> output, Reporter reporter)
					throws IOException {
		while (values.hasNext()) {
			emit(key, values.next(), output);
		}
	}

}