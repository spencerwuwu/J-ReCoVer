import os

from utils import *

def get_bmc_name(names, logs):
    index = 0
    for log in logs:
        if index == 0:
            index = 1
            names.append(log)
        elif index == 1:
            index = 0

def get_bmc_time(results, logs, timeout):
    index = 0
    flag = 0
    while index < len(logs):
        if flag == 0:
            flag = 1
            index += 1
            continue
        else:
            flag = 0

        results.append(parse_time(logs[index], timeout))

        index += 1

def parse_bmc(tex_f, timeout):
    print "Parsing bmc results"
    
    opt_logs = os.popen("cat ../bmc_out/Optimize.log").read().splitlines()
    bmc_logs = os.popen("cat ../bmc_out/BMC.log").read().splitlines()

    names = []
    opt_results = []
    bmc_results = []

    get_bmc_name(names, opt_logs)
    get_bmc_time(opt_results, opt_logs, timeout)
    get_bmc_time(bmc_results, bmc_logs, timeout)

    data_f = open("../latex/bmc.data", "w")
    write2_data(opt_results, bmc_results, data_f)
    data_f.close()


    tex_f.write("% BMC exp\n")
    tex_f.write("\\begin{table}\n")
    tex_f.write("\\centering\n")
    tex_f.write("\\begin{tabular}{|l|l|l|l|l|l|l|l|}\n")

    tex_f.write("\\hline\n")
    tex_f.write("& \multicolumn{3}{c|}{\small BMC} &	& \multicolumn{3}{c|}{\small Optimized} \\\\\n")

    tex_f.write("\\hline\n")
    tex_f.write("\small Lines & \small Average & \small Median & \small Timeout & Lines/Vars (jimple) & \small Average & \small Median & \small Timeout \\\\\n")

    tex_f.write("\\hline\n")
    tex_f.write("\\hline\n")

    i = 0
    index = 50
    line_set = ["72.85/65.7", "129.95/120.85", "186.9/175.6", "243.75/230.6", "300.55/285.85"]
    line_index = 0
    while i < len(opt_results):
        opt_result = []
        bmc_result = []
        j = 0
        while j < 20:
            opt_result.append(opt_results[i])
            bmc_result.append(bmc_results[i])
            i += 1
            j += 1
        tex_f.write(str(index) + " & " + get3_column(bmc_result, timeout) + " & " + line_set[line_index] + " & " + get3_column(opt_result, timeout) + "\\\\\n")
        line_index += 1
        index += 50

    tex_f.write("\\hline\n")
    tex_f.write("\end{tabular}\n")
    tex_f.write("\\label{tab:bmc}\n")
    tex_f.write("\\end{table}\n")
    tex_f.write("\n\n")

    tex_f.write("\\begin{tikzpicture}\n")
    tex_f.write("\\begin{loglogaxis}[%\n")
    tex_f.write("xmin=-5, xmax=" + str(timeout + 20) + ",\n")
    tex_f.write("ymin=-5, ymax=" + str(timeout + 20) + ",\n")
    tex_f.write("xlabel=J-ReCoVer,\n")
    tex_f.write("ylabel=BMC\n")
    tex_f.write("]\n")
    tex_f.write("\\addplot[scatter,only marks,%\n")
    tex_f.write("scatter src=explicit symbolic, mark=x]%\n")
    tex_f.write("file {bmc.data};\n")
    tex_f.write("\draw (-100,-100) -- (400, 400);\n")
    tex_f.write("\end{loglogaxis}\n")
    tex_f.write("\end{tikzpicture}\n")

    tex_f.write("\n\n\n")

