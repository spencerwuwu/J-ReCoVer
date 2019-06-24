#!/usr/bin/env python2
import sys
import os
from subprocess import Popen, PIPE
from numpy import median
from prettytable import PrettyTable


def get_all_avg(results):
    ret = None
    cnt = 0
    for val in results:
        cnt += 1
        if ret is None:
            ret = val
        else:
            for i in range(1, len(ret)):
                ret[i] += val[i]

    for i in range(1, len(ret)):
        ret[i] = round(ret[i]/cnt, 3)

    print("Average: %s %s %s %s %s %s" % (ret[1], ret[2], ret[3], ret[4], ret[5], ret[6]))

def get_sorted(results, index):
    ret = list(results)
    
    for i in range(len(ret)):
        for j in range(len(ret) -i - 1):
            if ret[j][index] < ret[j+1][index]:
                tmp = ret[j][index]
                ret[j][index] = ret[j+1][index]
                ret[j+1][index] = tmp


    for i in range(5):
        print("%s\t%s %s %s %s %s %s" % (ret[i][0], ret[i][1], ret[i][2], ret[i][3], ret[i][4], ret[i][5], ret[i][6]))



def main():
    global_path = os.path.dirname(os.path.abspath(__file__)) + "/"
    extracted_path = global_path + "extracted/"
    files = os.listdir(extracted_path)

    results = []
    cnt = 0

    for java in files:
        if ".java" not in java:
            continue
        target = "extracted/" + java 
        size = int(os.popen("wc %s" % target).read().strip().split(" ")[0])
        group = int(size / 10)
        cmd = "../../j-exp.py " + extracted_path + java

        compile_time = 0.0
        process_time = 0.0
        solver_time = 0.0
        total_time = 0.0

        for i in range(5):
            print(i, java)
            proc = Popen(cmd, shell=True, stdout=PIPE)
            res = proc.communicate()[0].splitlines()

            compile_time += float(res[0].split(" ")[1])
            process_time += float(res[1].split(" ")[1])
            solver_time += float(res[2].split(" ")[1])
            total_time += float(res[3].split(" ")[1])

            num_line = int(res[4].split(": ")[1])
            num_variable = int(res[5].split(": ")[1])
            num_if = int(res[6].split(" ")[1])

        compile_time /= 5
        process_time /= 5
        solver_time /= 5
        total_time /= 5

        compile_time = round(compile_time, 3)
        process_time = round(process_time, 3)
        solver_time = round(solver_time, 3)
        total_time = round(total_time, 3)

        print("%s %s %s %s %s %s %s" % (compile_time, process_time, solver_time, total_time, num_line, num_variable, num_if))
        results.append([java, compile_time, process_time, solver_time, total_time, num_line, num_variable, num_if])

        

    print("Longest executation time:")
    get_sorted(results, 4)
    print("Most num of lines:")
    get_sorted(results, 5)
    print("Most num of variables:")
    get_sorted(results, 6)
    print("Most num of if condition:")
    get_sorted(results, 7)

    get_all_avg(results)

if __name__ == "__main__":
    main()
