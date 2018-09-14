#!/bin/python2.7
import os

Command = "java -jar jsr.jar mavens/New_iiii/target/New_iiii-1.0-SNAPSHOT.jar "
Command_I_D_C = "java -jar jsr.jar mavens/IDcontext/target/New_iiii-1.0-SNAPSHOT.jar "
Command2 = "java -jar jsr.jar mavens/New/target/New_iiii-1.0-SNAPSHOT.jar "
Command3 = "java -jar jsr.jar mavens/Text/target/New_iiii-1.0-SNAPSHOT.jar " 
TRUE = "RESULT: Prove your reducer to be communicative"
FALSE = "RESULT: Prove your reducer to be NOT communicative"
UNKNOWN = "----"

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
    "Collector11_531_810_4_8",
    "Collector12_0_90_6_5",
    "Collector34_141_200_3_9",
    "Collector36_141_200_7_7",
    "Collector5_0_90_2_18"
]

Result_L_L_L_L = [
    FALSE,
    UNKNOWN,
    TRUE,
    TRUE,
    TRUE,
    TRUE
]

BenchMarks_I_D_C = [
    "context0_90_11_2",
    "context0_90_35_20"
]

Result_I_D_C = [
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
    FALSE,
    TRUE,
    TRUE,
    TRUE,
    FALSE,
    TRUE,
    TRUE,
    UNKNOWN,
    TRUE,
    TRUE,
    TRUE
]

BenchMarks_T_L_T_L = [
    "Collector141_200_1_17",
    "Collector201_270_5_4",
    "Collector91_140_4_19",
    "Collector91_140_5_11"
]

Result_T_L_T_L = [
    TRUE,
    TRUE,
    TRUE,
    TRUE
]

BenchMarks_T_T_T_T = [
"Collector0_90_10_10",
"Collector0_90_1_12",
"Collector0_90_13_5",
"Collector0_90_3_12",
"Collector0_90_3_14",
"Collector0_90_5_12",
"Collector141_200_10_2",
"Collector141_200_1_6",
"Collector141_200_4_4",
"Collector141_200_4_8",
"Collector141_200_9_20",
"Collector201_270_1_13",
"Collector271_360_1_2",
"Collector531_810_3_10",
"Collector811_1600_1_15",
"Collector811_1600_2_13",
"Collector91_140_5_7",
"Collector91_140_8_17",
"Collector91_140_8_19"
]

Result_T_T_T_T = [
    TRUE,
    UNKNOWN,
    UNKNOWN
]

def main():
    success = 0.0
    total = 0.0

    index = 0
    for target in BenchMarks_I_I_I_I:
        result = os.popen(Command + target + "| grep RESULT").read()
        if Result_I_I_I_I[index] in result:
            print "-"
            success += 1
        else:
            print "I_I_I_I " + target
        index += 1
        total += 1

    index = 0
    for target in BenchMarks_L_L_L_L:
        result = os.popen(Command + target + "| grep RESULT").read()
        if Result_L_L_L_L[index] in result:
            print "-"
            success += 1
        else:
            print "L_L_L_L " + target
        index += 1
        total += 1

    index = 0
    for target in BenchMarks_T_I_T_I:
        result = os.popen(Command2 + target + "| grep RESULT").read()
        if Result_T_I_T_I[index] in result:
            print "-"
            success += 1
        else:
            print "T_I_T_I " + target
        index += 1
        total += 1

    index = 0
    for target in BenchMarks_T_L_T_L:
        result = os.popen(Command2 + target + "| grep RESULT").read()
        if Result_T_L_T_L[index] in result:
            print "-"
            success += 1
        else:
            print "T_L_T_L " + target
        index += 1
        total += 1
    """
    index = 0
    for target in BenchMarks_I_D_C:
        result = os.popen(Command_I_D_C + target + "| grep RESULT").read()
        if Result_I_D_C[index] in result:
            print "-"
            success += 1
        else:
            print "I_D_C " + target
        index += 1
        total += 1
    """

    """

    index = 0
    for target in BenchMarks_T_T_T_T:
        result = os.popen(Command3 + target + "| grep RESULT").read()
        if Result_T_T_T_T[index] in result:
            print "-"
            success += 1
        else:
            print "T_T_T_T " + target
        index += 1
        total += 1
    """

    print "total: " + str(success) + "/" + str(total) + ", " + str(success*100/total) + "%"


if __name__ == "__main__":
    main()

