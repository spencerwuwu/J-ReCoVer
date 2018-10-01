# J-ReCoVer   
J-ReCoVer, short for "Java Reducer Commutative Verifier", is a verification tool for Map-Reduce program written in Java.
J-ReCoVer symbolic executes the function body of a reducer and tries to prove that the order of the inputs will not
affect the output.
For more details please visit [our website](http://jrecover.iis.sinica.edu.tw/).

## Installation

Run ```./install.sh``` to fetch and build the wrapper of J-ReCoVer. In ```BUILD/``` you can find two executable files named
```j-ReCoVer``` and ```j-recover.jar```. 

```j-ReCover``` performs the same fuction as our [online version](http://jrecover.iis.sinica.edu.tw/editor/1).
Place your code in ```reducer.java``` then execute, the script will try to compile your reducer and provide a clean result.

```j-recover.jar``` is the core engine of our tool. Compile your Map-Reduce program into an executable jar and analysis
with it. Running in this mode can see how we symbolic execute the function body, generate a formula, and check the result with [z3](https://github.com/Z3Prover/z3).


## Usage
```
$ ./j-ReCoVer
Usage: ./j-ReCoVer T1 T2 T3 T4 <Context/Collector> [reducer file]


$ java -jar j-recover.jar -h
 * Usage:     <*.jar> <classname> [options] 

 * Options:
    -h              help
    -c classpath    Set classpath (Optional if you had packed all libs into the jar)
    -s              Silence mode, only output Jimple code
    -g              Generate control flow graph
 * Example:
     $ java -jar j-recover.jar your_jar.jar reducer_classname
   Slience mode 
     $ java -jar j-recover.jar your_jar.jar reducer_classname -s
```


## Build j-recover.jar without the script
### Using Ant
```
$ cd j-ReCoVer/
$ ant
```
### Using eclipse
* Import the project into your workspace
* Include all jar files in `/lib` from `Project > Properties > Java Build Path > Libraries > Add External JARs...`
