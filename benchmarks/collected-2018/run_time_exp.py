#!/usr/bin/env python2
import sys
import os
from subprocess import Popen, PIPE
from numpy import median
from prettytable import PrettyTable


def get_median(elements):
    return round(median(elements), 3)

def get_average(elements):
    total = 0.0
    count = 0
    for element in elements:
        total += element
        count += 1
    return round(total / count, 3)

def get_array(results, key, index):
    res = []
    for elements in results[key]:
        res.append(elements[index])
    return res

def get_all_avg(results):
    ret = None
    cnt = 0
    for key, val in results.items():
        cnt += 1
        if ret is None:
            ret = val
        else:
            for i in range(len(ret)):
                ret[i] += val[i]

    for i in range(len(ret)):
        ret[i] = round(ret[i]/cnt, 3)

    print("Average: %s %s %s %s %s %s %s" % (val[0], val[1], val[2], val[3], val[4], val[5], val[6]))

def get_max(results, index):
    ret = None
    for key, val in results.items():
        if ret is None:
            ret = (key, val)
        else:
            if ret[1][index] < val[index]:
                ret = (key ,val)

    key, val = ret

    print("%s %s %s %s %s %s %s %s" % (key, val[0], val[1], val[2], val[3], val[4], val[5], val[6]))


def main():
    global_path = os.path.dirname(os.path.abspath(__file__)) + "/"
    extracted_path = global_path + "extracted/"
    files = os.listdir(extracted_path)

    groups = dict()
    results = dict()
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
        results[java] = [compile_time, process_time, solver_time, total_time, num_line, num_variable, num_if]
        

    print("Longest executation time:")
    get_max(results, 3)
    print("Most num of lines:")
    get_max(results, 4)
    print("Most num of variables:")
    get_max(results, 5)
    print("Most num of if condition:")
    get_max(results, 6)
    get_all_avg(results)
    """
        times = (compile_time, process_time, solver_time)
        # Group by line numbers
        if group in groups:
            groups[group].append(target)
            results[group].append(times)
        else:
            groups[group] = [target]
            results[group] = [times]
        cnt += 1

    keys = sorted(groups.keys())


    x = PrettyTable(["Lines", 
                     "Average (Compile)", "Average (Process)", "Average (Solver)", "Average(total)"
                     ,"Median (Compile)", "Median (Process)", "Median (Solver)", "Median (total)"
                    ])

    final_sets = dict()
    for key in keys:
        final = dict()
        array = get_array(results, key, 0)
        ca = get_average(array)
        cm = get_median(array)
        final["compile"] = (ca, cm)

        array = get_array(results, key, 1)
        pa = get_average(array)
        pm = get_median(array)
        final["process"] = (pa, pm)
        
        array = get_array(results, key, 2)
        sa = get_average(array)
        sm = get_median(array)
        final["smt"] = (sa, sm)

        final_sets[key] = final
        line = "%s-%s" % (key*10+1, key*10+9)
        x.add_row([line,
                   ca, pa, sa, ca+pa+sa,
                   cm, pm, sm, cm+pm+sm])

    print(x)
    """

if __name__ == "__main__":
    main()
