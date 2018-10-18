#!/usr/bin/python2.7
import sys
import os
from subprocess import Popen, PIPE


def main():
    files = os.listdir("./")
    slog = open("output.success.log", "w")
    flog = open("output.failed.log", "w")
    scount = 0
    count = 0

    for java in files:
        if "sum_even_odd.java" not in java:
            continue
        print java
        count += 1
        cmd = "../../j-ReCoVer " + java + " | grep RESULT"
        proc = Popen(cmd, shell=True, stdout=PIPE)
        result = proc.communicate()[0]


        if len(result) != 0:
            slog.write(java + ":\n")
            slog.write(result)
            slog.write("---------\n\n");
            scount += 1
        else:
            flog.write(java + ":\n")
            print "failed"


    slog.close()
    flog.close()
    print "total: " + str(count)
    print "done: " + str(scount)


if __name__ == "__main__":
    main()
