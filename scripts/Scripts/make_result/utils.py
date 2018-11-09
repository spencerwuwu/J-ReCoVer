import sys
import os
from numpy import median

def determine_result(results):
    result_0 = 0 # init to commutative
    result_1 = 0

    if len(results) < 2:
        return 2

    if "timeout" in results[0] or "no result" in results[0]:
        return 2
    elif "NOT" in results[0]:
        result_0 = 1 
    if "found" in results[1]:
        result_1 = 1

    if result_0 == result_1:
        return result_0
    else:
        return 2

def parse_result(logs):
    # parse log in the form of
    #   *.java
    #   RESULT: j-receover
    #   RESULT: j-not-recover
    #   RESULT: ...
    r_c = 0 # equation that checks related (2)
    r_n = 0
    r_u = 0
    a_c = 0 # equation that checks all variables (1)
    a_n = 0
    a_u = 0
    resultsr = []
    resultsa = []

    index = 0
    for log in logs:
        if index == 0:
            index = 1
        elif index == 1:
            index = 2
            resultsr.append(log)
        elif index == 2:
            index = 3
            resultsa.append(log)
        else:
            index = 0
            resultsr.append(log)
            resultsa.append(log)
            resultr = determine_result(resultsr)
            resulta = determine_result(resultsa)
            if resulta == 0:
                a_c += 1
            elif resulta == 1:
                a_n += 1
            else:
                a_u += 1
            resultsa = []

            if resultr == 0:
                r_c += 1
            elif resultr == 1:
                r_n += 1
            else:
                r_u += 1
            resultsr = []

    return r_c, r_n, r_u, a_c, a_n, a_u

def parse_time(time, timeout):
    minute = float(time.split("m")[0])
    second = float(time.split("m")[1].split("s")[0])
    total = minute * 60 + second
    if total > timeout:
        return timeout 
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

def get_timeout(elements, timeout):
    count = 0
    for element in elements:
        if element >= timeout:
            count += 1
    return count

def get3_column(results, timeout):
    return str(get_average(results)) + " & " + str(get_median(results)) + " & " + str(get_timeout(results, timeout))

def get2_column(results, timeout):
    return str(get_average(results)) + " & " + str(get_timeout(results, timeout))

def write2_data(resultsa, resultsb, target_f):
    index = 0
    while index < len(resultsa):
        target_f.write(str(resultsa[index]) + "\t" + str(resultsb[index]) + "\n")
        index += 1
