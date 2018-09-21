//https://searchcode.com/file/69347180/core/src/main/java/org/apache/mahout/classifier/bayes/common/BayesFeatureReducer.java#l-21

package reduce_test;

import java.io.IOException;
import java.util.Iterator;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.mapred.Reporter;

public class autoGenerator implements ReducerO<
T1
, LongWritable, 
T3
,
T4
> {

	public static void main(String[] args){
		for(int i=0; i<Common.maxcount ; i++){  
			ReducerO<
				T1
				, LongWritable, 
			T3
				,
			T4
				> reducer=new autoGenerator();
			Tester<
			T1
			, LongWritable, 
				T3
					,
				T4
					> tester=new Tester<
					T1
					, LongWritable, 
				T3
					,
				T4
					>();
			LongWritable[] solutionArray = { new LongWritable(-1L), new LongWritable(0), new LongWritable(2L), new LongWritable(3L) };
			try {
				tester.test(new 
						T1_
						, solutionArray, reducer);
			} catch (IOException | InterruptedException e1) {
				e1.printStackTrace();
			}
		}
	} 

	REDUCER

}
