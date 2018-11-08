#!/usr/bin/python2.7
import os
from prettytable import PrettyTable
from numpy import median

def parse_time(time):
    minute = float(time.split("m")[0])
    second = float(time.split("m")[1].split("s")[0])
    total = minute * 60 + second
    if total > 300:
        return 300
    else:
        return total

def output(a):
    for element in a:
        print element + "\t",
    print "\n",

def get_num(storage, filename):
    index = 0
    lines = os.popen("cat " + filename).read().split("\n")
    for line in lines:
        if index == 0:
            index = 1
        else:
            storage.append(parse_time(line))
            index = 0

def get_name(storage, filename):
    index = 0
    lines = os.popen("cat " + filename).read().split("\n")
    for line in lines:
        if index == 0:
            index = 1
            storage.append(line)
        else:
            index = 0

def get_average(elements):
    total = 0.0
    count = 0
    for element in elements:
        total += element
        count += 1
    return round(total / count, 1)

def get_median(elements):
    return round(median(elements), 1)

def get_timeout(elements):
    count = 0
    for element in elements:
        if element >= 300:
            count += 1
    return count

def main(name, optimize, string):
    x = PrettyTable(["Lines", "**Average (Opt)**", "Average (Str)"
        , "**Median (Opt)**", "Median (Str)"
        , "**Timeout (Opt)**", "Timeout (Str)"])
    index = 0
    opts = []
    strs = []
    line = 1
    while index < len(name):
        if len(name[index]) == 0:
            index += 1
            continue
        target = int(name[index].split("_")[0].replace("test", ""))

        if target > line:
            x.add_row([str(line * 50)
                , "**" + str(get_average(opts)) + "**", str(get_average(strs))
                , "**" + str(get_median(opts)) + "**", str(get_median(strs))
                , "**" + str(get_timeout(opts)) + "**", str(get_timeout(strs))])
            line = target
            opts = []
            strs = []

        opts.append(optimize[index])
        strs.append(string[index])
        index += 1

    x.add_row([str(line * 50)
        , "**" + str(get_average(opts)) + "**", str(get_average(strs))
        , "**" + str(get_median(opts)) + "**", str(get_median(strs))
        , "**" + str(get_timeout(opts)) + "**", str(get_timeout(strs))])

    print x

if __name__ == "__main__":
    name = []
    optimize = []
    string = []
    
    get_name(name, "Optimize.log")
    get_num(optimize, "Optimize.log")
    get_num(string, "String.log")

    main(name, optimize, string)
