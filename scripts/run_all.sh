#!/bin/bash

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

if [[ $SIMPLE -eq 0 ]]; then
	echo ""
	./run_optimize.sh long
	echo ""
	./run_bmc.sh 300
else
	echo ""
	./run_optimize.sh short
	echo ""
	./run_bmc.sh 180
fi

echo ""
cd ../
