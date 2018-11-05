#!/usr/bin/python2.7
import sys
import os
def parse_time(time):
    minute = float(time.split("m")[0])
    second = float(time.split("m")[1].split("s")[0])
    total = minute * 60 + second
    if total > 300:
        return 300
    else:
        return total

def main():
    target = open("String.log", "r")
    log = open("string.log", "w")
    no = 0
    for line in target:
        if no == 0:
            no += 1
        elif no == 1:
            no += 1
            log.write(line)
        elif no == 2:
            no += 1
            log.write(line)
        elif no == 3:
            no += 1
            log.write(line)
        elif no == 4:
            no = 0
            print parse_time(line)

    target.close()
    log.close()
    
if __name__ == "__main__":
    main()
