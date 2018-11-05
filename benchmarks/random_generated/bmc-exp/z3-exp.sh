#!/bin/bash

list="$(ls -l formulas/ | awk '{print $9}')"
log="formula.log"
resultDir="result-formulas"
rm -rf $resultDir/*
mkdir -p $resultDir
rm $log

for VAR in $list
do
	echo $VAR
	echo "$VAR:" >> $log
	result="$resultDir/$VAR.result"
	{ time timeout 300 z3 formulas/$VAR > $result || echo TIMEOUT > $result; } 2>&1 | grep real | awk {'print $2'} >> $log
done
