#!/bin/python2.7
import sys
import os
from subprocess import Popen, PIPE


def main():
    global_path = os.path.dirname(os.path.abspath(__file__)) + "/"
    extracted_path = global_path + "extracted/"
    files = os.listdir(extracted_path)
    log = open("output.log", "w")

    for java in files:
        print java
        cmd = "../../j-ReCoVer " + extracted_path + java + " | grep RESULT"
        proc = Popen(cmd, shell=True, stdout=PIPE)
        result = proc.communicate()[0]

        log.write(java + ":\n")
        log.write(result)
        log.write("---------\n\n");

    log.close()


if __name__ == "__main__":
    main()
