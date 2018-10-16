#!/usr/bin/python2.7
import sys
import os
from subprocess import Popen, PIPE

types = {"IntWritable", "LongWritable", "DoubleWritable", "FloatWritable", "Integer", "Long", "Double", "Float"}

def main():
    global_path = os.path.dirname(os.path.abspath(__file__)) + "/"
    filtered_path = global_path + "filtered/"
    os.system("mkdir -p " + filtered_path)
    for (root, directories, filenames) in os.walk(global_path + "base/"):
        for filename in filenames:
            f =  os.path.join(root, filename)
            if ".java" not in f:
                break

            output = f.split("/base/")[1].replace("/", "_")

            input_f = open(f, "r")
            keep = False
            has_reduce = False
            has_hadoop = False
            for line in input_f.readlines():
                if "hadoop" in line:
                    has_hadoop = True
                if "implements Reducer" in line or "extends Reducer" in line:
                    has_reduce = True

                if "Iterator<" in line:
                    for target in types:
                        if ("Iterator<" + target + ">") in line:
                            keep = True
                            break
                elif "Iterable<" in line:
                    for target in types:
                        if ("Iterable<" + target + ">") in line:
                            keep = True
                            break
                if keep: 
                    break

            input_f.close()
            input_f = open(f, "r")

            if has_hadoop and has_reduce and keep:
                output_f = open(filtered_path + output, "w")
                for line in input_f.readlines():
                    output_f.write(line)
                output_f.close()


if __name__ == "__main__":
    main()
