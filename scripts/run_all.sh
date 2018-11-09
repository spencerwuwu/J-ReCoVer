#!/bin/bash

set -e

SIMPLE="0"

function print_help {
	echo "SYNOPSIS"
	echo "  ./run_all.sh [OPTIONS]"
	echo ""
	echo "OPTIONS"
	echo "  -s"
	echo "    Short version of experiements"
	echo "  -h|--help"
	echo "    Display this message"
}

while [[ $# -gt 0 ]]
do
	key="$1"
	case $key in
		-h|--help)
			print_help
			exit 1
		;;
		-s)
			SIMPLE=1
			shift
		;;
		*)	
			print_help
			exit 1
		;;
	esac
done

clear

if [[ $SIMPLE -eq 0 ]]; then
	echo "Run full experiements..."
else
	echo "Run shorter version..."
fi

cd Scripts/
echo ""
./run_svcomp.sh
echo ""
./run_related.sh


if [[ $SIMPLE -eq 0 ]]; then
	echo ""
	./run_optimize.sh 300
	echo ""
	./run_bmc.sh 300
else
	echo ""
	./run_optimize.sh 180
	echo ""
	./run_bmc.sh 180
fi

echo ""

cd make_result/ 
if [[ $SIMPLE -eq 0 ]]; then
	echo "Generating report with latex..."
	./make_result.py 300
else
	echo "Generating report with latex..."
	./make_result.py 180
fi

cd ../latex
echo ""
echo "Compiling report..."
pdflatex result 2>&1 > peflatex.log && cp result.pdf ../../

cd ../
echo ""
echo "Finished! See result.pdf for results"

