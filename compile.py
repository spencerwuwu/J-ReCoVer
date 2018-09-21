#!/bin/python2.7
import os
import sys

Mvn = "mvn package -q -Dmaven.test.skip=true -B"
Generated = "reducers/src/main/java/reduce_test/autoGenerator.java"
Reducer = "reducer.java"


def main():
    T1 = sys.argv[1]
    T2 = sys.argv[2]
    T3 = sys.argv[3]
    T4 = sys.argv[4]
    Type = sys.argv[5]
    global_path = sys.argv[6]

    wrapper = global_path + "templates/"


    if "IntWritable" in T2:
        wrapper = wrapper + "IntWritable"
    elif "DoubleWritable" in T2:
        wrapper = wrapper + "DoubleWritable"
    elif "LongWritable" in T2:
        wrapper = wrapper + "LongWritable"
        
    if "Collector" in Type:
        wrapper = wrapper + "_o.java"
    else:
        wrapper = wrapper + "_c.java"

    wrapper_f = open(wrapper, "r")
    reducer_f = open(global_path + Reducer, "r")
    generated_f = open(global_path + Generated, "w")

    for line in wrapper_f.readlines():
        if "T1_" in line:
            if "IntWritable" in T1:
                generated_f.write("IntWritable(1)")
            elif "Text" in T1:
                generated_f.write("Text(\"1\")")
        elif "T1" in line:
            generated_f.write(T1)
        elif "T3" in line:
            generated_f.write(T3)
        elif "T4" in line:
            generated_f.write(T4)
        elif "REDUCER" in line:
            for line_r in reducer_f.readlines():
                generated_f.write(line_r)
        else:
            generated_f.write(line)

    wrapper_f.close()
    reducer_f.close()
    generated_f.close()
    
    result = os.popen("cd " + global_path + "reducers/ && " + Mvn).read()
    if len(result) != 0:
        print result

    

if __name__ == "__main__":
    if (len(sys.argv) != 7):
        print "Interal Error"
    else:
        main()

