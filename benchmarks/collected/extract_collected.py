#!/bin/python2.7
import sys
import os
from subprocess import Popen, PIPE


def main():
    global_path = os.path.dirname(os.path.abspath(__file__)) + "/extracted/"
    os.system("mkdir -p " + global_path)
    for (root, directories, filenames) in os.walk("collected/"):
        for filename in filenames:
            f =  os.path.join(root, filename)
            if ".java" not in f:
                break
            cmd = os.path.dirname(os.path.abspath(__file__)) + "/parse " + f
            proc = Popen(cmd, shell=True, stdout=PIPE)
            result = proc.communicate()[0]

            output = f.replace("./", "").replace("/", "_")
            output_f = open(global_path + output, "w")

            print "--------------------"
            print output
            for line in result.splitlines():
                print line
                output_f.write(line + "\n")
            
            output_f.close()


if __name__ == "__main__":
    main()
