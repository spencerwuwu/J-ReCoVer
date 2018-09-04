#!/bin/python2.7
import os

Command = "java -jar jsr.jar mavens/New_iiii/target/New_iiii-1.0-SNAPSHOT.jar "
Command2 = "java -jar jsr.jar mavens/New/target/New_iiii-1.0-SNAPSHOT.jar "
TRUE = "RESULT: Prove your reducer to be communicative"
FALSE = "RESULT: Prove your reducer to be NOT communicative"

##### Section One 
BenchMarks_I_I_I_I = [
    "collector0_90_1_7",
    "collector0_90_3_2",
    "collector141_200_9_5",
    "collector531_810_1_1_1",
    "collector531_810_1_1_2",
    "collector531_810_1_1_3"
]

Result_I_I_I_I = [
    TRUE,
    TRUE,
    TRUE,
    FALSE,
    TRUE,
    TRUE
]

BenchMarks_L_L_L_L = [
    "Collector11_0_90_6_4",
    "Collector12_0_90_6_5",
    "Collector36_141_200_7_7",
    "Collector5_0_90_2_18"
]

Result_L_L_L_L = [
    FALSE,
    TRUE,
    TRUE,
    TRUE
]

##### Section Two
BenchMarks_T_I_T_I = [
    "collector0_90_10_15",
    "collector0_90_1_14",
    "collector0_90_1_9",
    "collector0_90_3_8",
    "collector0_90_5_15",
    "collector0_90_5_6_2",
    "collector141_200_10_1",
    "collector141_200_9_2",
    "collector141_200_9_4",
    "collector91_140_10_3",
    "collector91_140_1_7",
    "collector91_140_7_16",
    "collector91_140_7_17"
]

Result_T_I_T_I = [
    TRUE,
    TRUE,
    TRUE,
    TRUE,
    TRUE,
    TRUE,
    FALSE,
    TRUE,
    TRUE,
    TRUE,
    TRUE,
    TRUE,
    TRUE
]

def main():
    """
    index = 0
    for target in BenchMarks_I_I_I_I:
        result = os.popen(Command + target + "| grep RESULT").read()
        if Result_I_I_I_I[index] in result:
            print "-"
        else:
            print "I_I_I_I " + target
        index += 1

    index = 0
    for target in BenchMarks_L_L_L_L:
        result = os.popen(Command + target + "| grep RESULT").read()
        if Result_L_L_L_L[index] in result:
            print "-"
        else:
            print "L_L_L_L " + target
        index += 1
    """
    index = 0
    for target in BenchMarks_T_I_T_I:
        result = os.popen(Command2 + target + "| grep RESULT").read()
        if Result_T_I_T_I[index] in result:
            print "-"
        else:
            print "T_I_T_I " + target
        index += 1


if __name__ == "__main__":
    main()

