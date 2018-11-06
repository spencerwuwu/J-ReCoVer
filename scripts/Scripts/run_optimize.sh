#!/bin/bash

set -e

output_folder="optimize_out"
folder="Testcases/Optimize/"

OPT_log="$output_folder/Optimize.log"
STR_log="$output_folder/String.log"
total="$(ls $folder | wc | awk '{print $1}')"

function clean_process {
	local _countJar="$(ps aux | grep jar | awk '{print $2}' | wc | awk '{print $1}')"
	local _countZ3="$(ps aux | grep z3 | awk '{print $2}' | wc | awk '{print $1}')"
	if (( $_countJar > 1 )); then
		echo kill jar
		ps aux | grep jar | awk '{system("kill -9" $2)}'
	fi
	if (( $_countZ3 > 1 )); then
		echo kill z3 
		ps aux | grep z3 | awk '{system("kill -9" $2)}'
	fi
}

function analysis { #"$folder" "$opt_log" "$str_log"
	local _list="$(ls -l $1 | awk '{print $9}')"
	local _count=0
	for _target in $_list; do
		echo -ne "Progress: $_count.0/$total"\\r
		echo "$_target" >> $2
		echo "$_target" >> $3
		{ time ../J-ReCoVer/j-ReCoVer $1/$_target > /dev/null;} 2>&1 | grep real | awk '{print $2}' >> $2
		clean_process
		echo -ne "Progress: $_count.5/$total"\\r

		{ time ../J-ReCoVer/j-Str-ReCoVer $1/$_target > /dev/null;} 2>&1 | grep real | awk '{print $2}' >> $3
		clean_process
		((_count+=1))
	done
}

echo "Starting Optimize Exp"

rm -rf $output_folder
mkdir $output_folder
touch $OPT_log
touch $STR_log

analysis $folder $OPT_log $STR_log
echo "Progress: $total.0/$total"
echo "Done!"

