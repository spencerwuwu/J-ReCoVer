#!/usr/bin/python2.7
import sys
import os

def main():
    output_f = open("output.failed.txt", "r")
    for line in output_f.readlines():
        if ":-" in line:
            line = line.split(":")[0]
            print line
            os.system("mv extracted/" + line + " uncompilable/")

    output_f.close()

if __name__ == "__main__":
    main()
