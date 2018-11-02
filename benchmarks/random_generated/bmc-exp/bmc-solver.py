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
            seg = seg + "_" + rep
        if len(result) == 0:
            result = seg
        else:
            result = result + " " + seg
    return result

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
    target_f = os.popen("./smt-bmc-generator " + filename).read()

    formulas.append("(declare-const input0 Int)\n")
    formulas.append("(declare-const input1 Int)\n")
    tmp = []

    for line in target_f.splitlines():
        if len(line) < 2:
            continue

        if "declare" in line:
            linea1 = parse_line(line, "a1")
            linea2 = parse_line(line, "a2")
            lineb1 = parse_line(line, "b1")
            lineb2 = parse_line(line, "b2")
            formulas.append(linea1)
            formulas.append(linea2)
            formulas.append(lineb1)
            formulas.append(lineb2)
        elif "MINMAX" in line:
            vmin = line.split(" ")[1].split(":")[0]
            vmax = line.split(" ")[1].split(":")[1]
            if output in vmax:
                output = vmax
            if "cur" in line:
                continue
            tmp.append("(= " + vmin + "_a2 " + vmax + "_a1 )")
            tmp.append("(= " + vmin + "_b2 " + vmax + "_b1 )")
            tmp.append("(= " + vmin + "_b1 " + vmin + "_a1 )")
        else:
            linea1 = parse_line(line, "a1")
            linea2 = parse_line(line, "a2")
            lineb1 = parse_line(line, "b1")
            lineb2 = parse_line(line, "b2")
            formula = "(and (= cur_i0_a1 input0) (= cur_i0_a2 input1))\n"
            formula = "(and (and (= cur_i0_a1 input0) (= cur_i0_a2 input1)) " + formula + ")\n"
            formula = "(and (and (= cur_i0_b1 input1) (= cur_i0_b2 input0)) " + formula + ")\n"
            for f in tmp:
                formula = "(and " + formula + " " + f + ")\n"

            formula = "(and " + formula + " " + linea1 + ")\n"
            formula = "(and " + formula + " " + linea2 + ")\n"
            formula = "(and " + formula + " " + lineb1 + ")\n"
            formula = "(and " + formula + " " + lineb2 + ")\n"

            formula = "(and " + formula + " (not (= " + output + "_a2 " + output + "_b2)))\n"
            formula = "(assert " + formula + ")\n"

            formulas.append(formula)


if __name__ == "__main__":
    if len(sys.argv) != 2:
        print "Usage: ./bmc-solver.py file.pre"
        exit(1)

    formulas = []
    main(sys.argv[1], formulas)
    formulas.append("(check-sat)\n")

    for line in formulas:
        print line
    """
    cmd = "z3 -in"
    proc = Popen(cmd, shell=True, stdin=PIPE, stdout=PIPE, stderr=PIPE)
    for line in formulas:
        proc.stdin.write(line)
    (log, result) = proc.communicate()
    proc.stdin.close()
    print log
    """

