#!/usr/bin/python2.7
import sys
import os
from subprocess import Popen, PIPE

TIMEOUT = 300

### Process timer
def wait_timeout(proc, timeout):
    start = time.time()
    end = start + timeout
    interval = .25
    while True:
        result = proc.poll()
        if result is not None:
            return True
        if time.time() >= end:
            os.killpg(os.getpgid(proc.pid), signal.SIGKILL)
            return False
        time.sleep(interval)

def parse_line(line, rep):
    segs = line.split(" ")
    result = ""
    for seg in segs:
        if "_" in seg:
            seg = seg + rep
        if len(result) == 0:
            result = seg
        else:
            result = result + " " + seg

def find_output(filename):
    target = filename.replace(".pre", ".java")
    target_f = open(target, "r")
    result = ""
    for line in target_f.readlines():
        if "collect" in line:
            result = line.split("(")[2].split(")")[0]
        else:
            continue

    target_f.close()

    return result

def main(filename, formulas):
    output = find_output(filename)
    target_f = os.popen("./z3-bmc-generator " + filename).read()

    formulas.append("(declare-const input0 Int)\n")
    formulas.append("(declare-const input1 Int)\n")
    tmp = []

    for line in target_f.splitlines():
        if "declar" in line
            linea1 = parse_line(line, "a1")
            linea2 = parse_line(line, "a2")
            lineb1 = parse_line(line, "b2")
            lineb2 = parse_line(line, "b2")
            formulas.append(linea1)
            formulas.append(linea2)
            formulas.append(linea1)
            formulas.append(lineb2)
        elif "MINMAX" in line:
            vmin = line.split(" ")[1].split(":")[0]
            vmax = line.split(" ")[1].split(":")[1]


if __name__ == "__main__":
    if len(sys.argv) != 2:
        print "Usage: ./bmc-solver.py file.pre"
        exit(1)

    formulas = []
    main(sys.argv[1], formulas)
