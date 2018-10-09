#!/usr/bin/python2.7

import json
import urllib2
import unicodedata
import sys
import os

def write_file(filename, lines, global_path, target_url):
    output = open(global_path + filename, "w")
    output.write("// " + target_url + "\n\n")
    for line in lines:
        output.write(line + "\n")

    output.close()

def get_code_and_deter(codeId, target, global_path, total):
    target_url = "https://searchcode.com/api/result/" + str(codeId) + "/"
    code = json.load(urllib2.urlopen("https://searchcode.com/api/result/" + str(codeId) + "/")).get('code')
    lines = unicodedata.normalize("NFKD", code).encode('ascii', 'ignore').split("\n")

    discard = False
    keep = False
    for line in lines:
        if "import" in line:
            if not (
                ("java" in line)
                or ("hadoop" in line)
                or ("junit" in line)
                or ("common" in line)
            ):
                print "  | Found: " + line
                discard = True
                break
            else:
                if "hive" in line:
                    print "  | Found: " + line
                    discard = True
                    break

        if "Iterator<" in line:
            if ("Iterator<" + target + ">") not in line:
                print "  | Found: " + line
                discard = True
                break
            else:
                keep = True

        if "Iterable<" in line:
            if ("Iterable<" + target + ">") not in line:
                print "  | Found: " + line
                discard = True
                break
            else:
                keep = True

        if " sum " in line:
            print "  | Found: " + line
            discard = True
            break

    if keep and not discard:
        print "  |+ Added"
        write_file(str(total) + ".java", lines, global_path, target_url)
        return True
    else:
        return False



def main():
    page = 0
    total = 0
    target = sys.argv[1]

    search_base = "bitbucket"

    global_path = os.path.dirname(os.path.abspath(__file__)) + "/" + search_base + "/" + target + "/"
    os.system("mkdir -p " + global_path)

    while page != "None":
        data = json.load(urllib2.urlopen("https://searchcode.com/api/codesearch_I/?q=public+void+reduce(&src=3&lan=23&loc=30&per_page=100&p=" + str(page)))

        result = data.get('results')
        page_new = data.get('nextpage')
        if page_new < page:
            break
        else:
            page = page_new
        print "=== page" + str(page) + " ==="

        for element in result:
            print "|-+ " + element['name']
            if get_code_and_deter(element['id'], target, global_path, total):
                total += 1

if __name__ == "__main__":
    if len(sys.argv) != 2:
        print "./grepcode value-type"
        exit(1)
    main()
