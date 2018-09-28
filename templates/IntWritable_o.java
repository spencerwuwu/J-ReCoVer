//https://searchcode.com/file/69347180/core/src/main/java/org/apache/mahout/classifier/bayes/common/BayesFeatureReducer.java#l-21

package reduce_test;

import java.io.IOException;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.Collections;  
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.mapred.Reporter;

public class autoGenerator implements ReducerO<
T1
, IntWritable, 
T3
,
T4
> {

	public static void main(String[] args){
		for(int i=0; i<Common.maxcount ; i++){  
			ReducerO<
				T1
				, IntWritable, 
			T3
				,
			T4
				> reducer=new autoGenerator();
			Tester<
			T1
			, IntWritable, 
				T3
					,
				T4
					> tester=new Tester<
					T1
					, IntWritable, 
				T3
					,
				T4
					>();
			IntWritable[] solutionArray = { new IntWritable(-1), new IntWritable(0), new IntWritable(1), new IntWritable(3) };
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
