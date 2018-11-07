
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



public class autoGenerator implements ReducerO<
T1
, 
    T2
    , 
    T3
    ,
    T4
    > {

        static Size J_RECOVER_ITER_NUM = new Size();
        public static void main(String[] args) {
            int length = 5;
            while (length < 15) {
                J_RECOVER_ITER_NUM.set(length);
                for (int i = 0; i < Common.maxcount; i++){  
                    INPUT0
                    for (int j = 0; j < length; j++) {
                        RANDOM
                    }
                    ReducerO<
                        T1
                        , 
                        T2
                            , 
                        T3
                            ,
                        T4
                            > reducer1=new autoGenerator();
                    ReducerO<
                        T1
                        , 
                        T2
                            , 
                        T3
                            ,
                        T4
                            > reducer2=new autoGenerator();
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
                        tester.test(
                                T1_
                                , solutionArray, reducer1, reducer2);
                    } catch (IOException | InterruptedException e1) {
                        e1.printStackTrace();
                    }
                }
            length += 2;
            } 
        }

REDUCER

}
