#!/bin/bash

output_folder="svcomp_out"
src_folder="Testcases/SV-COMP/"
targets=('rangesum' 'avg' 'max' 'sep' 'sum')
log="$output_folder/time.log"
slog="$output_folder/result.log"

function analysis { #"${targets[@]}"
	local _count=0
	local _targets=( "$@" )
	local _target=
	for _target in ${_targets[@]}; do
		echo -ne "Progress: $_count/5"\\r
		echo "$_target" >> $log
		echo "$_target" >> $slog
		{ time ../J-ReCoVer/j-ReCoVer $src_folder/$_target.java | grep RESULT >> $slog; } 2>&1 | grep real | awk {'print $2'} >> $log
		((_count+=1))
	done
}

echo "Starting SV-COMP Exp"

rm -rf $output_folder
mkdir $output_folder
touch $log $slog
analysis "${targets[@]}"

echo "Progress: 5/5"
echo "Done!"
