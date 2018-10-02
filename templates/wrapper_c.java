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
import org.apache.hadoop.io.FloatWritable;
import java.util.HashMap;
import java.util.Map;

public class autoGenerator implements ReducerC<
T1
, 
    T2
    >{

        public static void main(String[] args) {
            int length = 5;
            while (length < 15) {
                for (int i = 0; i < Common.maxcount; i++){  
                    INPUT0
                    for (int j = 0; j < length; j++) {
                        RANDOM
                    }
                    ReducerC<
                        T1
                        , 
                    T2
                        > reducer=new autoGenerator();
                    Tester<
                        T1
                        , 
                        T2
                            , 
                        T3
                            , 
                        T4
                            > tester=new Tester<
                            T1
                            , 
                        T2
                            , 
                        T3
                            ,
                        T4
                            >();
                    try {
                        tester.test(new 
                                T1_
                                , solutionArray, reducer);
                    } catch (IOException | InterruptedException e1) {
                        e1.printStackTrace();
                    }
                }
            length += 2;
            }
        }

        REDUCER

    }
