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

def write2seed(seed_f, java, result):
    target = "extracted/" + java
    reducer_f = open(target, "r")
    reducer = reducer_f.read().replace("\n", "\\n").replace("\t", "\\t")
    reducer = reducer.replace("\"", "\\\"").replace("\'", "\\\'").replace("\r", "")
    seed_f.write("Testcase.create(:name => \"" + java + "\", ")

    # append source
    orig_java = "filtered/" + java.split("_p")[0] + ".java"
    orig_f = open(orig_java, "r")
    src = orig_f.readlines()[0].replace("\n", "\\n")
    reducer = src + reducer
    orig_f.close()

    seed_f.write(":java => \"" + reducer + "\", ")
    reducer_f.close()

    t1, t2, t3, t4, r_type = get_type(target)
    seed_f.write(":t1 => \"" + t1 + "\", ")
    seed_f.write(":t2 => \"" + t2 + "\", ")
    seed_f.write(":t3 => \"" + t3 + "\", ")
    seed_f.write(":t4 => \"" + t4 + "\", ")
    seed_f.write(":r_type => \"" + r_type + "\", ")
    seed_f.write(":source => \"2018\", ")
    seed_f.write(":comment => \"\", ")

    if type1 in result:
        result = "Proved to be commutative."
        seed_f.write(":result_type => 1, ")
    elif type2 in result:
        seed_f.write(":result_type => 2, ")
    else:
        seed_f.write(":result_type => 3, ")
    seed_f.write(":result => \"" + result + "\"")
    seed_f.write(")\n");



def main():
    seed_f = open("2_benchmarks2018.rb", "w")
    log_f = open("output.success.txt", "r")

    java = ""
    result = ""
    for line in log_f.readlines():
        if "java" in line:
            java = line.split(":")[0]
            result = ""

        if "RESULT" in line:
            result += line.replace("RESULT: ", "").replace("\n", "") + ". "
        if "---------" in line:
            result = result.replace(" in 500 tests", "").replace(":.", ".")
            write2seed(seed_f, java, result)


    seed_f.write("puts \"Benchmarks 2018 done\"")
    seed_f.close()

if __name__ == "__main__":
    main()
