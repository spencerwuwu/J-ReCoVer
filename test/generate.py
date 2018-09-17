#!/bin/python2.7
import os

Command = "java -jar jsr.jar build/reducers/target/New_iiii-1.0-SNAPSHOT.jar "
Path = "reducers/src/main/java/reduce_test/benchmark"
TRUE = "RESULT: Prove your reducer to be communicative"
FALSE = "RESULT: Prove your reducer to be NOT communicative"
UNKNOWN = "----"

def main():

    files = os.listdir(Path)
    for reducer in files:
        if (("collector" in reducer) 
            or ("context" in reducer) 
            or (("Collector" in reducer and "Output" not in reducer))):
            reducer = reducer.replace(".java", "")

            os.system("cp -r reducers/ testing/" + reducer)
            template = open("template.xml", "r")
            target = open("testing/" + reducer + "/pom.xml", "w")
            for line in template.readlines():
                if "TODO" in line:
                    target.write("<mainClass>reduce_test." + reducer + "</mainClass>\n")
                else:
                    target.write(line)

            template.close()
            target.close()
            os.system("cd testing/" + reducer + "&& mvn package")



if __name__ == "__main__":
    main()

