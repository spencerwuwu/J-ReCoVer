import os

from utils import *

def get_optimize_name(names, logs):
    index = 0
    for log in logs:
        if index == 0:
            index = 1
            names.append(log)
        elif index == 1:
            index = 0

def get_optimize_time(results, logs, slogs, timeout):
    index = 0
    flag = 0
    while index < len(logs):
        if flag == 0:
            flag = 1
            index += 1
            continue
        else:
            flag = 0

        if "1" not in slogs[index]:
            results.append(timeout)
        else:
            results.append(parse_time(logs[index], timeout))

        index += 1


def parse_optimized(tex_f, timeout):
    print "Parsing optimize results"

    opt_logs = os.popen("cat ../optimize_out/Optimize.log").read().splitlines()
    str_logs = os.popen("cat ../optimize_out/String.log").read().splitlines()

    opt_slogs = os.popen("cat ../optimize_out/optimize.log").read().splitlines()
    str_slogs = os.popen("cat ../optimize_out/string.log").read().splitlines()

    names = []
    opt_results = []
    str_results = []

    get_optimize_name(names, opt_logs)
    get_optimize_time(opt_results, opt_logs, opt_slogs, timeout)
    get_optimize_time(str_results, str_logs, str_slogs, timeout)

    data_f = open("../latex/optimize.data", "w")
    write2_data(opt_results, str_results, data_f)
    data_f.close()
    

    tex_f.write("% Optimize exp\n")
    tex_f.write("\\begin{table}\n")
    tex_f.write("\\centering\n")
    tex_f.write("\\begin{tabular}{|l|l|l|l|l|l|l|}\n")

    tex_f.write("\\hline\n")
    tex_f.write("& \multicolumn{3}{c|}{\small w/o Optimization}	& \multicolumn{3}{c|}{\small Optimized} \\\\\n")

    tex_f.write("\\hline\n")
    tex_f.write("\small Lines & \small Average & \small Median & \small Timeout & \small Average & \small Median & \small Timeout \\\\\n")

    tex_f.write("\\hline\n")
    tex_f.write("\\hline\n")

    i = 0
    index = 50
    while i < len(opt_results):
        opt_result = []
        str_result = []
        j = 0
        while j < 60:
            opt_result.append(opt_results[i])
            str_result.append(str_results[i])
            i += 1
            j += 1
        tex_f.write(str(index) + " & " + get3_column(str_result, timeout) + " & " + get3_column(opt_result, timeout) + "\\\\\n")
        index += 50

    tex_f.write("\\hline\n")
    tex_f.write("\end{tabular}\n")
    tex_f.write("\\label{tab:opt1}\n")
    tex_f.write("\\end{table}\n")
    tex_f.write("\n\n")

    tex_f.write("\\begin{tikzpicture}\n")
    tex_f.write("\\begin{axis}[%\n")
    tex_f.write("xmin=-5, xmax=" + str(timeout + 20) + ",\n")
    tex_f.write("ymin=-5, ymax=" + str(timeout + 20) + ",\n")
    tex_f.write("xlabel=Optimize,\n")
    tex_f.write("ylabel=w/o Optimization\n")
    tex_f.write("]\n")
    tex_f.write("\\addplot[scatter,only marks,%\n")
    tex_f.write("scatter src=explicit symbolic, mark=x]%\n")
    tex_f.write("file {optimize.data};\n")
    tex_f.write("\draw (-100,-100) -- (400, 400);\n")
    tex_f.write("\end{axis}\n")
    tex_f.write("\end{tikzpicture}\n")

    tex_f.write("\n\n\n")



