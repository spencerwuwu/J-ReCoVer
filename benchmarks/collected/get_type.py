#!/usr/bin/python2.7
import os

type1 = "Proved to be commutative"
type2 = "CANNOT prove to be commutative. Counterexample found"
type3 = "CANNOT prove to be commutative. Cannot find a counterexample"
type4 = "Proved to be commutative. Counterexample found"

def parse_param1(target):
    tokens = target.split(" ")
    if (len(tokens) == 2):
        return tokens[0]
    else:
        return tokens[1]

def parse_param2(target):
    return target.split("<")[1].split(">")[0]

def parse_param_o(target):
    tokens = target.split("<")[1].split(">")[0].replace(" ", "").split(",")
    return tokens[0], tokens[1]

def get_type(target):
    target_f = open(target, "r")
    t1 = ""
    t2 = ""
    t3 = ""
    t4 = ""
    r_type = ""
    lines = target_f.read().replace("\r", "").replace("\n", "").replace("\t", "").split("public void reduce")[1]
    params = lines.split("(")[1].split(")")[0].split(",")

    if (len(params) == 3):
        r_type = "Context"
        t1 = parse_param1(params[0])
        t2 = parse_param2(params[1])
        t3 = t1
        t4 = t2
    elif (len(params) == 5):
        r_type = "Collector"
        t1 = parse_param1(params[0])
        t2 = parse_param2(params[1])
        t3, t4 = parse_param_o(params[2] + "," +  params[3])
    target_f.close()
    #print t1 + " " + t2 + " " + t3 + " " + t4 + " " + r_type
    return t1, t2, t3, t4, r_type

def write2seed(java):
    target = "extracted/" + java
    t1, t2, t3, t4, r_type = get_type(target)
    print t4



def main():
    log_f = open("output.success.txt", "r")

    java = ""
    for line in log_f.readlines():
        if "java" in line:
            java = line.split(":")[0]

        if "---------" in line:
            write2seed(java)


if __name__ == "__main__":
    main()
