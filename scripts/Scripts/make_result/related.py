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

    tex_f.write("% Equation exp\n")
    tex_f.write("\\begin{table}[htb]\n")
    tex_f.write("\\centering\n")
    tex_f.write("\\begin{tabular}{|l|l|l|l|}\n")
    tex_f.write("\\hline\n")
    tex_f.write("& &W/O optimization	& With optimization\\\\\n")


    logs = os.popen("cat ../related_out/CO.log").read().splitlines()
    r_c, r_n, r_u, a_c, a_n, a_u = parse_result(logs)

    tex_f.write("\\hline\n")
    tex_f.write("\\hline\n")
    tex_f.write("\multirow{4}{*}{Open repositories}&Commutative& " + str(a_c) + "&" + str(r_c) + " \\\\\n")
    tex_f.write("\cline{2-4}\n")
    tex_f.write("&Non-commutative&" + str(a_n) + "&" + str(r_n) + "\\\\\n")
    tex_f.write("\cline{2-4}\n")
    tex_f.write("&Unknown&" + str(a_u) + "&" + str(r_u) + "\\\\\n")
    tex_f.write("\cline{2-4}\n")
    sums = r_c + r_n + r_u
    p_r = round( float(r_c + r_n)/sums * 100, 1)
    p_a = round( float(a_c + a_n)/sums * 100, 1)
    tex_f.write("&Precision& " + str(p_a) + "\% & " + str(p_r) + "\%\\\\\n")
    tex_f.write("\\hline\n")

    logs = os.popen("cat ../related_out/LI.log").read().splitlines()
    r_c, r_n, r_u, a_c, a_n, a_u = parse_result(logs)

    tex_f.write("\\hline\n")
    tex_f.write("\multirow{3}{*}{Literatures}&Commutative& " + str(a_c) + "&" + str(r_c) + " \\\\\n")
    tex_f.write("\cline{2-4}\n")
    tex_f.write("&Non-commutative&" + str(a_n) + "&" + str(r_n) + "\\\\\n")
    tex_f.write("\cline{2-4}\n")
    tex_f.write("&Unknown&" + str(a_u) + "&" + str(r_u) + "\\\\\n")
    tex_f.write("\cline{2-4}\n")
    sums = r_c + r_n + r_u
    p_r = round( float(r_c + r_n)/sums * 100, 1)
    p_a = round( float(a_c + a_n)/sums * 100, 1)
    tex_f.write("&Precision& " + str(p_a) + "\% & " + str(p_r) + "\%\\\\\n")
    tex_f.write("\\hline\n")

    tex_f.write("\end{tabular}\n")
    tex_f.write("\caption{The improvement of precision using data flow analysis. The row ``Precision'' is calculated as the\n")
    tex_f.write(" number of examples that J-Recover returns commutative or non-commutative divided by the total number of example.}\n")
    tex_f.write("\\label{tab:opt1}\n")
    tex_f.write("\\end{table}\n")
    tex_f.write("\n\n\n")

