#!/bin/bash

output_folder="bmc_out"
folder="Testcases/BMC/"
timeout=$1

OPT_log="$output_folder/Optimize.log"
BMC_log="$output_folder/BMC.log"
total="$(ls -l $folder | awk '{print $9}' | grep .bmc | sed 's/\.bmc//g' | wc -l)"

function clean_process {
	local _countJar="$(ps aux | grep jar | awk '{print $2}' | wc -l)"
	local _countZ3="$(ps aux | grep z3 | awk '{print $2}' | wc -l)"
	if (( $_countJar > 1 )); then
		echo kill jar
		ps aux | grep jar | awk '{system("kill -9" $2)}'
	fi
	if (( $_countZ3 > 1 )); then
		echo kill z3 
		ps aux | grep z3 | awk '{system("kill -9" $2)}'
	fi
}

function analysis { #"$folder" "$opt_log" "$bmc_log"
	local _list="$(ls -l $folder | awk '{print $9}' | grep .bmc | sed 's/\.bmc//g')"
	local _count=0
	for _target in $_list; do
		echo -ne "Progress: $_count.0/$total"\\r
		echo "$_target" >> $2
		echo "$_target" >> $3
		{ time timeout $timeout z3 $1/$_target.jz3 > /dev/null;} 2>&1 | grep real | awk '{print $2}' >> $2
		clean_process
		echo -ne "Progress: $_count.5/$total"\\r

		{ time timeout $timeout z3 $1/$_target.bmc > /dev/null;} 2>&1 | grep real | awk '{print $2}' >> $3
		clean_process
		((_count+=1))
	done
}

echo "Starting BMC Exp with timeout $timeout"

rm -rf $output_folder
mkdir $output_folder
touch $OPT_log
touch $BMC_log

analysis $folder $OPT_log $BMC_log
echo "Progress: $total.0/$total"
echo "Done!"

