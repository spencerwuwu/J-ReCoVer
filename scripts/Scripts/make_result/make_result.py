#!/usr/bin/python2.7
import sys
import os

from utils import *
from svcomp import *
from related import *
from optimized import *
from bmc import *

def init_pdf(tex_f):
    tex_f.write("\documentclass{article}\n")
    tex_f.write("\usepackage{tikz,pgfplots}\n")
    tex_f.write("\usepackage{multirow}\n")
    tex_f.write("\\begin{document}\n")


def finish_pdf(tex_f):
    tex_f.write("\end{document}\n")


if __name__ == "__main__":
    timeout = int(sys.argv[1])
    tex_f = open("../latex/result.tex", "w")
    init_pdf(tex_f)

    parse_svcomp(tex_f)
    parse_related(tex_f)
    parse_optimized(tex_f, timeout)
    parse_bmc(tex_f, timeout)

    finish_pdf(tex_f)
