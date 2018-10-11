#!/usr/bin/python2.7
import sys
import os

def main(path):
    count = 0
    for (root, directories, filenames) in os.walk(path):
        for filename in filenames:
            f =  os.path.join(root, filename)
            if ".java" in f:
                count += 1


    print "total: " + str(count)


if __name__ == "__main__":
    if len(sys.argv) < 2:
        print "./count_java.py dir"
        exit(1)
    main(sys.argv[1])
