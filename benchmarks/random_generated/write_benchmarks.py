#!/usr/bin/python2.7
import sys
import os
import time
from subprocess import Popen, PIPE


def generate(if_num):
    path = "s2/"
    no = 0
    while no < 10:
        target = path + "test" + str(if_num) + "_" + str(no) + ".java "
        re = os.system("./generator " + target + " " + str(if_num) + " " + str(if_num * 10) + " " + str(if_num))
        time.sleep(1)
        while re != 0:
            re = os.system("./generator " + target + " " + str(if_num) + " " + str(if_num * 10) + " " + str(if_num))
            time.sleep(1)
        no += 1 
    

def main():
    if_num = 10 
    while if_num <= 100:
        generate(if_num)
        if_num += 10 

if __name__ == "__main__":
    main()
