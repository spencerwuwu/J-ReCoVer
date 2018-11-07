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
    #   RESULT: ...
    #   RESULT: ...
    c = 0
    n = 0
    u = 0
    results = []
    init_flag = 0
    for log in logs:
        if ".java" in log:
            if init_flag == 0:
                init_flag = 1
                continue
            result = determine_result(results)
            if result == 0:
                c += 1
            elif result == 1:
                n += 1
            else:
                u += 1
            results = []
        else:
            results.append(log)

    result = determine_result(results)
    if result == 0:
        c += 1
    elif result == 1:
        n += 1
    else:
        u += 1

    return c, n, u

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
    return total / count

def get_median(elements):
    return median(elements)

def get_timeout(elements, timeout):
    count = 0
    for element in elements:
        if element >= timeout:
            count += 1
    return count

def get3_column(results, timeout):
    return str(get_average(results)) + " & " + str(get_median(results)) + " & " + str(get_timeout(results, timeout))

def write2_data(resultsa, resultsb, target_f):
    index = 0
    while index < len(resultsa):
        target_f.write(str(resultsa[index]) + "\t" + str(resultsb[index]) + "\n")
        index += 1
