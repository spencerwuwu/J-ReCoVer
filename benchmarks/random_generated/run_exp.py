#!/usr/bin/python2.7
import sys
import os
from subprocess import Popen, PIPE

def write_set(log, Matrix, Count, w):
    index = 0
    while index < w:
        number = 0.0
        log.write("test " + str(index * 10 + 10) + ":\n")
        for ans in Matrix[index]:
            log.write(str(ans) + "\n")
            number += ans

        log.write("----------\n")
        if (Count[index] == 0):
            log.write("avg: -\n")
        else:
            log.write("avg: " + str(number / Count[index]) + "\n")
        index += 1
        log.write("==========\n")

def write_ans2set(Matrix, Count, index, ans):
    index2 = Count[index]
    Count[index] += 1
    Matrix[index][index2] = ans
    

def main():
    files = os.listdir("s2/")
    slog = open("output.log", "w")
    w = (100) / 10
    h = 10
    Matrix = [[0.0 for x in range(h)] for y in range(w)]
    Count = [0 for x in range(w)]

    for java in files:
        print java
        cmd = "time ../../j-ReCoVer " + "s2/" + java + " | grep RESULT"
        #cmd = "time cat " + "s2/" + java + " | grep void"

        set_index = (int(java.replace("test", "").split("_")[0])-10) / 10

        proc = Popen(cmd, shell=True, stdout=PIPE, stderr=PIPE)
        (log, result) = proc.communicate()
        print log
        if len(log.splitlines()) < 2:
            ans = 300
            print ans
            write_ans2set(Matrix, Count, set_index, ans)
            continue
        elif "timeout" in log:
            ans = 300
            print ans
            write_ans2set(Matrix, Count, set_index, ans)
            continue

        for line in result.splitlines():
            if "user" in line:
                ans = float(line.split("user")[0])
                print ans
                write_ans2set(Matrix, Count, set_index, ans)
                break

    write_set(slog, Matrix, Count, w)

    slog.close()


if __name__ == "__main__":
    main()
