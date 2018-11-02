#!/usr/bin/python2.7
import sys
import os

def main():
    files = os.listdir("benchmarks/")
    for java in files:
        if ".java" not in java:
            continue

        print java
        result = os.popen("../../j-Formula benchmarks/" + java).read()
        z3 = java.replace(".java", ".jz3")
        target_f = open("formulas/" + z3, "w")
        target_f.write(result)
        target_f.close()

        bmc = java.replace(".java", ".bmc")
        pre = java.replace(".java", ".pre")
        result = os.popen("./bmc-solver.py benchmarks/" + pre).read()
        target_f = open("formulas/" + bmc, "w")
        target_f.write(result)
        target_f.close()


if __name__ == "__main__":
    os.system("rm -rf formulas/ && mkdir -p formulas/")
    main()
