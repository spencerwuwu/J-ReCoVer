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
echo ""
./run_optimize.sh
echo ""
./run_bmc.sh
echo ""
cd ../
