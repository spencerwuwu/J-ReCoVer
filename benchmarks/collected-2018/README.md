## Usage:
```grep_code.py```: Calling searchcode api.   
```filter.py```: Place your codes searched with ```grep_code.py```
into a dir name ```base```. This script finds the code in ```base```
that operates in number and copy to ```filtered```.   
```extract_raw.py```: Extract the reducer from ```filtered``` to
```estracted```. A program in ```filtered``` may contain several
reducers.
```run_exp.py```: Analysis the reducers in ```extracted``` with
J-ReCoVer.


## Notes:   
The benchmarks that contain self-defined data structures are put in ```uncompilable```.   
The following benchmarks are modified a little to fix compile errors.

github_50-250_94_p0.java   
github_50-250_211_p0.java   
bitbucket_250-450_50_p0.java   
bitbucket_250-450_64_p0.java   
github_250-450_44_p1.java    
github_250-450_44_p0.java   
github_50-250_549_p0.java   
github_50-250_666_p0.java    
github_50-250_663_p0.java    
