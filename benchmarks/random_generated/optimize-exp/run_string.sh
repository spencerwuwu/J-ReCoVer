#!/bin/bash

list="$(ls -l benchmarks/ | awk '{print $9}')"
log="String.log"
slog="string.log"
rm -rf $log
rm -rf $slog

for VAR in $list
do
	echo $VAR
	echo "$VAR:" >> $log
	echo "$VAR:" >> $slog
	{ time ../exp/j-ReCoVer-o benchmarks/$VAR | grep RESULT >> $slog; } 2>&1 | grep real | awk {'print $2'} >> $log

	countJar="$(ps aux | grep jar | awk '{print $2}' | wc | awk '{print $1}')"
	countZ3="$(ps aux | grep z3 | awk '{print $2}' | wc | awk '{print $1}')"
	if (( $countJar > 1 )); then
		echo kill jar
		ps aux | grep jar | awk '{system("kill -9" $2)}'
	fi
	if (( $countZ3 > 1 )); then
		echo kill z3 
		ps aux | grep z3 | awk '{system("kill -9" $2)}'
	fi
done

