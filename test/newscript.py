#!/bin/python2.7
import os

Command = "java -jar jsr.jar build/reducers/target/New_iiii-1.0-SNAPSHOT.jar "
Path = "build/reducers/src/main/java/reduce_test/"
TRUE = "RESULT: Prove your reducer to be communicative"
FALSE = "RESULT: Prove your reducer to be NOT communicative"
UNKNOWN = "----"

def main():
    success = 0.0
    total = 0.0

    files = os.listdir(Path)
    for reducer in files:
        if (("collector" in reducer) 
            or ("context" in reducer) 
            or (("Collector" in reducer and "Output" not in reducer))):
            reducer = reducer.replace(".java", "")
            result = os.popen(Command + reducer + "| grep RESULT").read()
            total += 1

            if (len(result) == 0):
                print "+ " + reducer
                print "UNKNOWN"

            if (TRUE in result):
                print "-"
                success += 1
            else:
                print "+ " + reducer
                print "UNKNOWN"


    print "total: " + str(success) + "/" + str(total) + ", " + str(success*100/total) + "%"


if __name__ == "__main__":
    main()

