//https://searchcode.com/file/100327042/hadoop-mapreduce-project/hadoop-mapreduce-client/hadoop-mapreduce-client-jobclient/src/test/java/org/apache/hadoop/mapreduce/TestMapReduce.java#l-39

package reduce_test;

import java.io.IOException;
import org.apache.hadoop.io.IntWritable;

public class context361_530_1_6_1 implements ReducerC<IntWritable, IntWritable>{

	public static void main(String[] args){
		for(int i=0; i<Common.maxcount ; i++){  
			ReducerC<IntWritable, IntWritable> reducer=new context361_530_1_6_1();
			Tester<IntWritable, IntWritable, IntWritable, IntWritable> tester=new Tester<IntWritable, IntWritable, IntWritable, IntWritable>();
			IntWritable[] solutionArray = { new IntWritable(1), new IntWritable(2), new IntWritable(3) };
			try {
				tester.test(new IntWritable(1), solutionArray, reducer);
			} catch (IOException | InterruptedException e1) {
				e1.printStackTrace();
			}
		}
	}

		  public void reduce(IntWritable key, Iterable<IntWritable> it,
			        Context context) throws IOException, InterruptedException {
			      for (IntWritable iw : it) {
			        context.write(iw, null);
			      }
		  }

}
