#!/usr/bin/python2.7
import sys
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

def output(log, a):
    for element in a:
        log.write(str(element) + "\t")
    log.write("\n")

def parse_log(names, jres, bmcs):
    log = open("formula.log", "r")
    name = ""
    bmc_time = 0.0
    j_time = 0.0
    n = 0
    for line in log.readlines():
        if n == 0:
            name = line.split(".")[0]
            n += 1
        elif n == 1:
            bmc_time = parse_time(line)
            n += 1
        elif n == 3:
            j_time = parse_time(line)
            n = 0
            names.append(name)
            jres.append(j_time)
            bmcs.append(bmc_time)
        else:
            n += 1
            continue
    log.close()

def parse_jre_lines(jre_lines, jre_vars):
    lines = os.popen("cat soot.log | awk '{print $2}'").read()
    variables = os.popen("cat soot.log | awk '{print $3}'").read()
    for line in lines.splitlines():
        jre_lines.append(int(line))
    for line in variables.splitlines():
        jre_vars.append(int(line))

def main(names, jres, bmcs, jre_lines, jre_vars):
    log = open("time.dat", "w")
    x = PrettyTable(["Lines/Vars", "Lines/Vars(J-ReCoVer)"
        , "**Average (J-ReCoVer)**", "Average (BMC)"
        , "**Median (J-ReCoVer)**", "Median (BMC)"
        , "**Timeout (J-ReCoVer)**", "Timeout (BMC)"])
    line = 1
    index = 0
    jres_l = []
    bmcs_l = []
    lines = []
    jvars = []
    a = []
    while index < len(names):
        target = int(names[index].split("_")[0].replace("test", ""))

        if target > line:
            x.add_row([str(line * 50) + "/" + str(line * 5), str(get_average(lines)) + "/" + str(get_average(jvars))
                , "**" + str(get_average(jres_l)) + "**", str(get_average(bmcs_l))
                , "**" + str(get_median(jres_l)) + "**", str(get_median(bmcs_l))
                , "**" + str(get_timeout(jres_l)) + "**", str(get_timeout(bmcs_l))])
            line = target
            jres_l = []
            bmcs_l = []
            lines = []
            jvars = []
        jres_l.append(jres[index])
        bmcs_l.append(bmcs[index])
        lines.append(jre_lines[index])
        jvars.append(jre_vars[index])
        a = [names[index], jres[index], bmcs[index]]
        index += 1
        output(log, a)
    log.close()

    x.add_row([str(line * 50) + "/" + str(line * 5), str(get_average(lines)) + "/" + str(get_average(jvars))
        , "**" + str(get_average(jres_l)) + "**", str(get_average(bmcs_l))
        , "**" + str(get_median(jres_l)) + "**", str(get_median(bmcs_l))
        , "**" + str(get_timeout(jres_l)) + "**", str(get_timeout(bmcs_l))])

    print x


if __name__ == "__main__":
    names = []
    jres = []
    bmcs = []
    jre_lines = []
    jre_vars = []
    parse_log(names, jres, bmcs)
    parse_jre_lines(jre_lines, jre_vars)
    main(names, jres, bmcs, jre_lines, jre_vars)
