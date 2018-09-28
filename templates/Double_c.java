//https://searchcode.com/file/100948365/Mapreduce/Programs/StockMinMaxReducer.java#l-6

package reduce_test;

import java.io.IOException;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.Collections;  
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;

public class autoGenerator implements ReducerC<
T1
, Double>{

	public static void main(String[] args){
		for(int i=0; i<Common.maxcount ; i++){  
			ReducerC<
				T1
				, Double> reducer=new autoGenerator();
			Tester<
				T1
				, Double, 
				T3
					, 
				T4
					> tester=new Tester<
				T1
				, Double, 
				T3
					,
				T4
					>();
			Double[] solutionArray = {-1.5, 0, 2.5, 3.5 };
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
