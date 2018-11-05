# Random Benchmark Generation

Two experiements are conducted here. The `optimize-exp` compares the 
scalability between our current version and the old, string-based 
version. The `bmc-exp` compares the smt-formula J-ReCoVer generates 
and the smt formula generated directed from benchmarks with 
[bounded model checking](http://fmv.jku.at/bmc/).

For our approach to generate random benchmarks, please visit 
[here](https://github.com/spencerwuwu/benchmark-generator).   
There are some issues in the process for distrubing if-else,
I just re-generated another one if the assertions in the 
generator failed.

For the scalability experiment we remove “/ %” because these 
operations would be too hard for SMT solver.   
For "*" it has a 1% chance to be used and it is always **V * N** 
where N is an integer.   
For bmc-exp I removed unary operations for convienent.
