#!/bin/bash

function plot_graph {
	cat <<-EOF | gnuplot
	set output "optimize.jpeg"
	set terminal jpeg
	set logscale xy
	set xlabel "Optimize (s)
	set ylabel "String (s)
	set xr [3.0:350.0]
	set yr [3.0:350.0]
	set arrow from 3,3 to 350,350 nohead
	plot "$1" using $2:$3 title "Optimize vs String"
	EOF
}

plot_graph time.dat 2 3
