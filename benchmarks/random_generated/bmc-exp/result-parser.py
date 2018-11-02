#!/usr/bin/python2.7
import sys
import os
from prettytable import PrettyTable


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

def main():
    log = open("formula.log", "r")
    n = 0
    name = ""
    bmc_time = ""
    j_time = ""
    bmc_result = ""
    j_result = ""
    x = PrettyTable(["name", "bmc_time", "j_time",
        "bmc_result", "j_result"])
    a = []
    for line in log.readlines():
        if n == 0:
            name = line.split(".")[0]
            n += 1
        elif n == 1:
            bmc_time = str(parse_time(line))
            n += 1
        elif n == 3:
            j_time = str(parse_time(line))
            bmc_result = os.popen("cat result-formulas/" + name + ".bmc.result").read().replace("\n", "")
            j_result = os.popen("cat result-formulas/" + name + ".jz3.result").read().replace("\n", "")
            n = 0
            x.add_row([name, bmc_time, j_time, bmc_result, j_result])
            a = [name, bmc_time, j_time, bmc_result, j_result]
            #output(a)
        else:
            n += 1
            continue

    print x

if __name__ == "__main__":
    main()
