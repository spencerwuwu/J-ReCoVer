| Lines | Average (Compile) | Average (Process) | Average (Solver) | Average (total) | Median (Compile) | Median (Process) | Median (Solver) | Median (total) |
|-------|-------------------|-------------------|------------------|-----------------|------------------|------------------|-----------------|----------------|
|  1-9  |       2.962       |       1.101       |       0.03       |      4.093      |      2.966       |      1.002       |      0.033      |     4.001      |
| 11-19 |       2.969       |        1.14       |      0.032       |      4.141      |      2.976       |      1.253       |      0.036      |     4.265      |
| 21-29 |       2.987       |       1.223       |      0.036       |      4.246      |      2.978       |      1.253       |      0.038      |     4.269      |
| 31-39 |       2.986       |       1.228       |      0.029       |      4.243      |       2.97       |      1.253       |      0.026      |     4.249      |
| 41-49 |       3.078       |       1.253       |      0.039       |       4.37      |      3.078       |      1.253       |      0.039      |      4.37      |


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
