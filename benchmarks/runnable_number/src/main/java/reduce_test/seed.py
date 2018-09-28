#!/usr/bin/python2.7
import os

def main():
    files = os.listdir("./")
    seed_f = open("seeds.rb", "w")
    seed_f.write("DatabaseCleaner.clean_with(:truncation)\n\n")
    for reducer in files:
        if (("collector" in reducer) 
            or ("context" in reducer) 
            or (("Collector" in reducer and "Output" not in reducer))):
            print reducer
            r_type = ""
            T1 = ""
            T2 = ""
            T3 = ""
            T4 = ""
            seed_f.write("Testcase.create(:name => \"" + reducer + "\", ")

            seed_f.write(":java => \"")

            result = os.popen("./a.out " + reducer).readlines()
            for line in result:
                line = line.replace("\r\n", "")
                line = line.replace("\n", "")
                seed_f.write(line)
                if "Context" in line:
                    r_type = "Context"
                if "Collector" in line:
                    r_type = "Collector"

                if "reduce(" in line:
                    parts = line.split(",")
                    T1 = parts[0].split("(")[1].split(" ")[0]
                    T2 = parts[1].split("<")[1].split(">")[0]
                    print "\t" + T1 + ", "  + T2
                if "OutputCollector" in line:
                    part = line.split("OutputCollector")[1].replace(" ", "").split("<")[1].split(">")[0]
                    T3 = part.split(",")[0]
                    T4 = part.split(",")[1]
                    print "\t" + T3 + ", "  + T4


            seed_f.write("\", :t1 => \"" + T1)
            seed_f.write("\", :t2 => \"" + T2)
            seed_f.write("\", :t3 => \"" + T3)
            seed_f.write("\", :t4 => \"" + T4 + "\", ")
            seed_f.write(":r_type => \"" + r_type + "\", :result => \"Proved to be communicable\")\n")

            
            #break
    seed_f.write("puts \"done\"")
    seed_f.close()

if __name__ == "__main__":
    main()
