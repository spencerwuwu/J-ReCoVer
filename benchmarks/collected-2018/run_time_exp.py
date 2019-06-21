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
        proc = Popen(cmd, shell=True, stdout=PIPE)
        res = proc.communicate()[0].splitlines()

        compile_time = float(res[0].split(" ")[1])
        process_time = float(res[1].split(" ")[1])
        solver_time = float(res[2].split(" ")[1])
        times = (compile_time, process_time, solver_time)
        print(times)
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

if __name__ == "__main__":
    main()
