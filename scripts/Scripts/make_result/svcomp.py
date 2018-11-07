import os

from utils import *

def parse_svcomp(tex_f):
    print "Parsing svcomp results"
    logs = os.popen("cat ../svcomp_out/time.log").read().splitlines()
    data = []
    index = 0
    for line in logs:
        if index == 0:
            index = 1
            continue
        else:
            index = 0
            data.append(parse_time(line, 300))

    tex_f.write("% SVCOMP exp\n")
    tex_f.write("\\begin{table}\n")
    tex_f.write("\\centering\n")
    tex_f.write("\\begin{tabular}{|l|l|l|l|l|l|l|}\n")
    tex_f.write("\\hline\n")
    tex_f.write("& \small CBMC\cite{cbmc2} 	& \small CPA-Seq\cite{cpachecker} & \small  ESBMC-kind\cite{esbmc} ")
    tex_f.write("& \small  UAutomizer\cite{uautomizer} &\small  VeriAbs\cite{veriabs} &\small J-Recover\\\\\n")
    tex_f.write("\\hline\n")
    tex_f.write("\\hline\n")
    tex_f.write("\small \textsf{rangesum}	& 0.96 & 30  & 2.2 &  10 & 180 & " + str(data[0]) + "\\\\\n")
    tex_f.write("\\hline\n")
    tex_f.write("\small \textsf{avg}	& TO & TO & TO & TO & 2 & " + str(data[1]) + "\\\\\n")
    tex_f.write("\\hline\n")                               
    tex_f.write("\small \textsf{max}	& TO & TO & TO & TO & 7  & " + str(data[2]) + "\\\\\n")
    tex_f.write("\\hline\n")                               
    tex_f.write("\small \textsf{sep}	& TO & TO & TO & TO & TO  & " + str(data[3]) + "\\\\\n")
    tex_f.write("\\hline\n")                               
    tex_f.write("\small \textsf{sum}	& TO & TO & TO & TO & 1.7  & " + str(data[4]) + "\\\\\n")
    tex_f.write("\\hline\n")
    tex_f.write("\end{tabular}\n")
    tex_f.write("\\label{tab:svcomp}\n")
    tex_f.write("\\end{table}\n")
    tex_f.write("\n\n\n")

