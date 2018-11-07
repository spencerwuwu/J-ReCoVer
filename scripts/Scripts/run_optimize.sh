#!/bin/bash

output_folder="optimize_out"
folder="Testcases/Optimize/"
version=$1

OPT_log="$output_folder/Optimize.log"
STR_log="$output_folder/String.log"
OPT_slog="$output_folder/optimize.log"
STR_slog="$output_folder/string.log"
total="$(ls $folder | wc -l)"

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

function analysis { #"$folder" "$opt_log" "$str_log" "$opt_slog" "$str_slog"
	local _list="$(ls -l $1 | awk '{print $9}')"
	local _count=0
	for _target in $_list; do
		echo -ne "Progress: $_count.0/$total"\\r
		echo "$_target" >> $2
		echo "$_target" >> $3
		echo "$_target" >> $4
		echo "$_target" >> $5
		(time { ../J-ReCoVer/j-ReCoVer-$version $1/$_target 2>&1 | grep RESULT | wc -l >> $4 ;};) 2>&1 | grep real | awk '{print $2}' >> $2
		clean_process
		echo -ne "Progress: $_count.5/$total"\\r

		(time { ../J-ReCoVer/j-Str-ReCoVer-$version $1/$_target 2>&1 | grep RESULT | wc -l >> $5 ;};) 2>&1 | grep real | awk '{print $2}' >> $3
		clean_process
		((_count+=1))
	done
}

if [[ $version == "long" ]]; then
	echo "Starting Optimize Exp with timeout 300"
else
	echo "Starting Optimize Exp with timeout 180"
fi

rm -rf $output_folder
mkdir $output_folder
touch $OPT_log
touch $OPT_slog
touch $STR_log
touch $STR_slog

analysis $folder $OPT_log $STR_log $OPT_slog $STR_slog
echo "Progress: $total.0/$total"
echo "Done!"

