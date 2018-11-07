#!/bin/bash

output_folder="related_out"
LI_folder="Testcases/Literacture/"
CO_folder="Testcases/Collected/"

LI_r_log="$output_folder/LI_r.log"
LI_n_log="$output_folder/LI_n.log"
CO_r_log="$output_folder/CO_r.log"
CO_n_log="$output_folder/CO_n.log"

function analysis { #"$folder" "$r_log" "n_log"
	local _list="$(ls -l $1 | awk '{print $9}')"
	local _num="$(ls $1 | wc -l)"
	local _count=0
	for _target in $_list; do
		echo -ne "Progress: $_count.0/$_num"\\r
		echo "$_target" >> $2
		echo "$_target" >> $3
		../J-ReCoVer/j-ReCoVer $1/$_target | grep RESULT >> $2
		echo -ne "Progress: $_count.5/$_num"\\r
		../J-ReCoVer/j-Not-ReCoVer $1/$_target | grep RESULT >> $3
		((_count+=1))
	done
}

echo "Starting Equations Exp"

rm -rf $output_folder
mkdir $output_folder
touch $LI_n_log $CO_n_log $LI_r_log $CO_r_log

echo "Literacture:"
analysis $LI_folder $LI_r_log $LI_n_log
echo "Progress: 10.0/10"
echo "Done!"

echo "Open repositories:"
analysis $CO_folder $CO_r_log $CO_n_log
echo "Progress: 118.0/118"
echo "Done!"

