#!/usr/bin/python2.7
import os

def parse_time(time):
    minute = float(time.split("m")[0])
    second = float(time.split("m")[1].split("s")[0])
    total = minute * 60 + second
    if total > 300:
        return 300
    else:
        return total

def output(a):
    for element in a:
        print element + "\t",
    print "\n",

def get_num(storage, filename):
    index = 0
    lines = os.popen("cat " + filename).read().split("\n")
    for line in lines:
        if index == 0:
            index = 1
        else:
            storage.append(parse_time(line))
            index = 0

def get_name(storage, filename):
    index = 0
    lines = os.popen("cat " + filename).read().split("\n")
    for line in lines:
        if index == 0:
            index = 1
            storage.append(line)
        else:
            index = 0

def main(name, optimize, string):
    index = 0
    while index < len(name):
        if len(name[index]) == 0:
            break
        a = [name[index], str(optimize[index]), str(string[index])]
        output(a)
        index += 1

if __name__ == "__main__":
    name = []
    optimize = []
    string = []
    
    get_name(name, "Optimize.log")
    get_num(optimize, "Optimize.log")
    get_num(string, "String.log")

    main(name, optimize, string)
