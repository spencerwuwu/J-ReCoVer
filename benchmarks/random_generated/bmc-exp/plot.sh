#!/bin/bash

function plot_graph {
	cat <<-EOF | gnuplot
	set output "bmc.jpeg"
	set logscale xy
	set terminal jpeg
	set xlabel "J-ReCoVer (s)
	set ylabel "BMC (s)
	set xr [-5.0:320.0]
	set yr [-5.0:320.0]
	set arrow from 0.1,0.1 to 200,200 nohead
	plot "$1" using $2:$3 title "J-ReCoVer vs BMC"
	EOF
}

plot_graph time.dat 2 3
