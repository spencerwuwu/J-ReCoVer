#!/bin/bash

output_folder="related_out"
LI_folder="Testcases/Literacture/"
CO_folder="Testcases/Collected/"

LI_log="$output_folder/LI.log"
CO_log="$output_folder/CO.log"

function analysis { #"$folder" "$r_log"
	local _list="$(ls -l $1 | awk '{print $9}')"
	local _num="$(ls $1 | wc -l)"
	local _count=0
	for _target in $_list; do
		echo -ne "Progress: $_count.0/$_num"\\r
		echo "$_target" >> $2
		../J-ReCoVer/j-Not-ReCoVer $1/$_target | grep RESULT >> $2
		((_count+=1))
	done
}

echo "Starting Opt1 Exp"

rm -rf $output_folder
mkdir $output_folder
touch $LI_log $CO_log

echo "Literacture:"
analysis $LI_folder $LI_log
echo "Progress: 10.0/10"
echo "Done!"

echo "Open repositories:"
analysis $CO_folder $CO_log
echo "Progress: 118.0/118"
echo "Done!"

