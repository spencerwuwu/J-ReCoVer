#!/bin/bash

function plot_graph {
	cat <<-EOF | gnuplot
	set output "optimize.jpeg"
	set terminal jpeg
	set xlabel "Optimize (s)
	set ylabel "String (s)
	set xr [0.0:320.0]
	set yr [0.0:320.0]
	set arrow from 0,0 to 320,320 nohead
	plot "$1" using $2:$3 title "Optimize vs String"
	EOF
}

plot_graph time.dat 2 3
