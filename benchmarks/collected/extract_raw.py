#!/usr/bin/python2.7
import sys
import os
from subprocess import Popen, PIPE

types = {"IntWritable", "LongWritable", "DoubleWritable", "FloatWritable", "Integer", "Long", "Double", "Float"}

def main():
    global_path = os.path.dirname(os.path.abspath(__file__)) + "/"
    extracted_path = global_path + "extracted/"
    os.system("mkdir -p " + extracted_path)
    for (root, directories, filenames) in os.walk(global_path + "filtered/"):
        for filename in filenames:
            f =  os.path.join(root, filename)
            if ".java" not in f:
                break
            cmd = global_path + "parse " + f
            proc = Popen(cmd, shell=True, stdout=PIPE)
            result = proc.communicate()[0]

            output = f.replace(".java", "").replace("filtered", "extracted")

            count = 0
            new_flag = False
            output_f = open(output + "_p" + str(count) + ".java", "w")
            # print "--------------------"
            for line in result.splitlines():
                #print line
                if "++++++++" in line:
                    if not new_flag:
                        new_flag = True
                        count += 1
                    else:
                        output_f.close()
                        output_f = open(output + "_p" + str(count) + ".java", "w")
                        count += 1
                else:
                    output_f.write(line + "\n")
            
            output_f.close()

def parse_param2(target):
    return target.split("<")[1].split(">")[0]


def filter_reducer():
    global_path = os.path.dirname(os.path.abspath(__file__)) + "/"
    extracted_path = global_path + "extracted/"
    files = os.listdir(extracted_path)

    for java in files:
        target = extracted_path + java
        target_f = open(target, "r")
        lines = target_f.read().replace("\r", "").replace("\n", "").replace("\t", "")
        if "public void reduce" not in lines:
            os.system("rm -f " + target)
            continue

        lines = lines.split("public void reduce")[1]
        params = lines.split("(")[1].split(")")[0].split(",")
        t2 = parse_param2(params[1])
        target_f.close()

        if t2 not in types:
            os.system("rm -f " + target)



if __name__ == "__main__":
    main()
    filter_reducer()
