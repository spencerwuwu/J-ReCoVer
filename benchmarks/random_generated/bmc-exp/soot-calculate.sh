#!/bin/bash

list="$(ls -l benchmarks/ | grep java | awk '{print $9}')"
log="soot.log"
rm -f $log

for VAR in $list
do
	result="$(../../../j-Jimple benchmarks/$VAR)"
	echo "$VAR	$result" | tee -a $log
done
