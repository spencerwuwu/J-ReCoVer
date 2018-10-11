//https://searchcode.com/file/100948365/Mapreduce/Programs/StockMinMaxReducer.java#l-6

package reduce_test;

/*
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.IOException;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.Random;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.Collections;  

import junit.framework.TestCase;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.FloatWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.io.SequenceFile;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.WritableComparable;
import org.apache.hadoop.io.SequenceFile.CompressionType;
import org.apache.hadoop.mapred.lib.IdentityMapper;
import org.apache.hadoop.mapred.lib.IdentityReducer;
import org.apache.hadoop.mapred.Reporter;
import org.apache.hadoop.mapred.JobClient;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
*/
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
