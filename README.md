# J-ReCoVer 
## Installation
Notice: Soot runs under JDK1.7
### Using Ant
```
$ cd j-ReCoVer/
$ ant
```
### Using eclipse
* Import the project into your workspace
* Include all jar files in `/lib` from `Project > Properties > Java Build Path > Libraries > Add External JARs...`

## Usage
```
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
