//https://searchcode.com/file/100948365/Mapreduce/Programs/StockMinMaxReducer.java#l-6

package reduce_test;

import java.io.*;
import java.util.*;  

import junit.framework.*;

import org.apache.hadoop.conf.*;
import org.apache.hadoop.fs.*;
import org.apache.hadoop.io.*;
import org.apache.hadoop.io.SequenceFile.CompressionType;
import org.apache.hadoop.mapred.*;
import org.apache.hadoop.util.*;

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
                    KEYGEN
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
