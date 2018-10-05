#!/bin/bash

function check_dep() { # "${dependencies[@]}"
	echo "Checking dependencies..."
	local _sat=1
	local _packages=( "$@" )
	local _package=
	for _package in ${_packages[@]}; do
		if [ "$(command -v $_package)" = "" ]; then
			echo "Requires: $_package"
			_sat=0
		fi
	done
	if [ $_sat -eq 0 ]; then
		exit 1
	fi
	echo "Dependency statisfied"
	echo ""
}

function build_jrecover() {
	echo "Compiling J-ReCoVer..."
	ant
	echo ""
}

function fetch_wrapper() {
	echo "Building J-ReCoVer Wrapper..."
	rm -rf BUILD
	mkdir BUILD
	cd BUILD
	git init
	git remote add origin "https://github.com/spencerwuwu/J-ReCoVer_wrapper.git"
	git pull origin master --depth=1
	cp ../j-recover.jar ./
	mv sample_reducer.java reducer.java

	echo ""
	echo "Testing J-ReCoVer, the result should be commutative..."
	
	if [ "$(./j-ReCoVer Text IntWritable Text IntWritable Collector | grep Proved)" = "" ]; then
		echo "The result didn't correspond, something went wrong"
		exit 1
	fi
	cd ../
}

dependencies=('git' 'java' 'mvn' 'ant' 'python2' 'z3')

check_dep "${dependencies[@]}"
build_jrecover
fetch_wrapper

echo ""
echo "Complete installation in BUILD/"
