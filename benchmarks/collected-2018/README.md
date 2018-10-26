## Usage:
```grep_code.py```: Calling searchcode api.   
```filter.py```: Place your codes searched with ```grep_code.py```
into a dir name ```base```. This script finds the code in ```base```
that operates in number and copy to ```filtered```.   
```extract_raw.py```: Extract the reducer from ```filtered``` to
```estracted``` and remove duplicated benchmarks. A program in 
```filtered``` may contain several reducer functions.
```run_exp.py```: Analysis the reducers in ```extracted``` with
J-ReCoVer.


## Notes:   
The benchmarks that contain self-defined data structures are put in ```uncompilable```.   
Some benchmarks are modified a little to fix compile errors.
