# ReducerAnalysis 
## Installation
Notice: Soot runs under JDK1.7
### Using Ant
```
$ cd ReducerAnalysis/
$ ant
```
### Using eclipse
* Import the project into your workspace
* Include all jar files in `/lib` from `Project > Properties > Java Build Path > Libraries > Add External JARs...`

## Usage
```
usage: Input [options] 
* Input: class/jar/directory 
* Options:
    -h               help 
    -c class_path    Set classpath (Optional for jar file) 
    -s 			         Silence mode, only output Jimple 
    -g               Generate control flow graph 
* Example:
    Analysis jar file 
    $ java -jar jsr.jar your_jar_file.jar
    Analysis directory 
    $ java -jar jsr.jar input_path/ -c classpath/ 
    Slience mode \n
    $ java -jar jsr.jar your_jar_file.jar\n -s
```
