#!/bin/python2.7
import sys
import os
from subprocess import Popen, PIPE


def main():
    global_path = os.path.dirname(os.path.abspath(__file__)) + "/"
    extracted_path = global_path + "extracted/"
    os.system("mkdir -p " + extracted_path)
    for (root, directories, filenames) in os.walk(global_path + "raw/"):
        for filename in filenames:
            f =  os.path.join(root, filename)
            if ".java" not in f:
                break
            cmd = global_path + "parse " + f
            proc = Popen(cmd, shell=True, stdout=PIPE)
            result = proc.communicate()[0]

            output = f.split("/raw/")[1].replace("/", "_").replace(".java", "")

            count = 0
            new_flag = False
            output_f = open(extracted_path + output + "_" + str(count) + ".java", "w")
            # print "--------------------"
            # print output
            for line in result.splitlines():
                # print line
                if "public void reduce" in line:
                    if not new_flag:
                        new_flag = True
                        count += 1
                    else:
                        output_f.close()
                        output_f = open(extracted_path + output + "_" + str(count) + ".java", "w")
                        count += 1
                output_f.write(line + "\n")
            
            output_f.close()


if __name__ == "__main__":
    main()
