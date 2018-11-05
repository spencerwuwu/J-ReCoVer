#!/bin/bash

list="$(ls -l extracted/ | awk '{print $9}')"
log="String.log"
olog="String.log"

rm -rf $log && touch $log
rm -rf $olog && touch $olog

for VAR in $list
do
	echo $VAR
	echo "$VAR:" >> $log
	echo "$VAR:" >> $olog
	{ time ../../j-ReCoVer extracted/$VAR | grep RESULT >> $olog; } 2>&1 | grep real | awk {'print $2'} >> $log
done
