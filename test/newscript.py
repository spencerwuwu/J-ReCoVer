#!/bin/python2.7
import os

Path = "reducers/src/main/java/reduce_test/benchmark"
TRUE = "RESULT: Prove your reducer to be communicative"
FALSE = "RESULT: Prove your reducer to be NOT communicative"
UNKNOWN = "----"

def print_result(result_list, output):
    count = 0
    for reducer in result_list:
        output.write("| " + reducer)
        count += 1
    output.write("| \n")
    output.write("| " + str(count) + "\n")
    output.write("\n")

def main():
    total = 0.0
    
    both_comm = []
    soot_comm = []
    test_comm = []
    none_comm = []

    files = os.listdir(Path)

    for reducer in files:
        if (("collector" in reducer) 
            or ("context" in reducer) 
            or (("Collector" in reducer and "Output" not in reducer))):
            reducer = reducer.replace(".java", "")
            print "+ " + reducer

            comm_soot = False
            comm_test = False
            total += 1
            path = "testing/" + reducer + "/target/New-1.jar "

            result_soot = os.popen("java -jar jsr.jar " + path + reducer + "| grep RESULT").read()
            if (len(result_soot) == 0):
                print "soot: UNKNOWN"
            elif (TRUE in result_soot):
                print "-"
                comm_soot = True
            else:
                print "soot: UNKNOWN"

            result_test = os.popen("java -jar " + path + "| grep RESULT").read()
            if ("NOT" in result_test):
                print "test: Not communicabale"
            else:
                print "-"
                comm_test = True

            if comm_soot:
                if comm_test:
                    both_comm.append(reducer + "\n")
                else:
                    soot_comm.append(reducer + "\n")
            else:
                if comm_test:
                    test_comm.append(reducer + "\n")
                else:
                    none_comm.append(reducer + "\n")
            #break


    output = open("output.log", "w")

    output.write("total: " + str(total) + "\n")
    output.write("both communicative:\n")
    print_result(both_comm, output)

    output.write("soot communicative:\n")
    print_result(soot_comm, output)

    output.write("test communicative:\n")
    print_result(test_comm, output)

    output.write("both not communicative:\n")
    print_result(none_comm, output)

    output.close()

    #print "total: " + str(success) + "/" + str(total) + ", " + str(success*100/total) + "%"
    print "total: " + str(total)


if __name__ == "__main__":
    main()

