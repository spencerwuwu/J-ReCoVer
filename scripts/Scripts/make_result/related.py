import os

from utils import *

def parse_related(tex_f):
    print "Parsing equation results"

    a_c = 0 # equation that checks all variables (1)
    a_n = 0
    a_u = 0
    r_c = 0 # equation that checks related (2)
    r_n = 0
    r_u = 0

    logs = os.popen("cat ../related_out/CO_n.log").read().splitlines()
    a_c, a_n, a_u = parse_result(logs)

    logs = os.popen("cat ../related_out/CO_r.log").read().splitlines()
    r_c, r_n, r_u = parse_result(logs)

    tex_f.write("% Equation exp\n")
    tex_f.write("\\begin{table}\n")
    tex_f.write("\\centering\n")
    tex_f.write("\\begin{tabular}{|l|l|l|l|}\n")
    tex_f.write("\\hline\n")
    tex_f.write("& &Equation~(1)	& Equation~(2) \\\\\n")

    tex_f.write("\\hline\n")
    tex_f.write("\\hline\n")
    tex_f.write("\multirow{3}{*}{Open repositories}&Commutative& " + str(a_c) + "&" + str(r_c) + " \\\\\n")
    tex_f.write("\cline{2-4}\n")
    tex_f.write("&Non-commutative&" + str(a_n) + "&" + str(r_n) + "\\\\\n")
    tex_f.write("\cline{2-4}\n")
    tex_f.write("&Unknown&" + str(a_u) + "&" + str(r_u) + "\\\\\n")

    logs = os.popen("cat ../related_out/LI_n.log").read().splitlines()
    a_c, a_n, a_u = parse_result(logs)

    logs = os.popen("cat ../related_out/LI_r.log").read().splitlines()
    r_c, r_n, r_u = parse_result(logs)

    tex_f.write("\\hline\n")
    tex_f.write("\\hline\n")
    tex_f.write("\multirow{3}{*}{Literatures}&Commutative& " + str(a_c) + "&" + str(r_c) + " \\\\\n")
    tex_f.write("\cline{2-4}\n")
    tex_f.write("&Non-commutative&" + str(a_n) + "&" + str(r_n) + "\\\\\n")
    tex_f.write("\cline{2-4}\n")
    tex_f.write("&Unknown&" + str(a_u) + "&" + str(r_u) + "\\\\\n")
    tex_f.write("\\hline\n")
    tex_f.write("\end{tabular}\n")
    tex_f.write("\\label{tab:svcomp}\n")
    tex_f.write("\\end{table}\n")
    tex_f.write("\n\n\n")

