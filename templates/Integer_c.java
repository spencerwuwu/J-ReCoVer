//https://searchcode.com/file/100948365/Mapreduce/Programs/StockMinMaxReducer.java#l-6

package reduce_test;

import java.io.IOException;
import java.util.Iterator;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;

public class autoGenerator implements ReducerC<
T1
, Integer>{

	public static void main(String[] args){
		for(int i=0; i<Common.maxcount ; i++){  
			ReducerC<
				T1
				, Integer> reducer=new autoGenerator();
			Tester<
				T1
				, Integer, 
				T3
					, 
				T4
					> tester=new Tester<
				T1
				, Integer, 
				T3
					,
				T4
					>();
			Integer[] solutionArray = {-1, 0, 1, 3 };
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
