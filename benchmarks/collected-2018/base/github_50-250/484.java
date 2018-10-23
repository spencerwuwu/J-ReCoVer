// https://searchcode.com/api/result/98860497/

package invindx;
import java.io.IOException;
import java.util.StringTokenizer;
import java.io.File;
import java.io.PrintStream;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IOUtils;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.GenericOptionsParser;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Collections;
import java.util.Iterator;
  public class InvindxReducer      
       extends Reducer<Text,Text,Text,Text> {
String op;
private Text result = new Text();
 public static final String FS_PARAM_NAME = "fs.defaultFS";

    public void reduce(Text key, Iterable<Text> values, 
                       Context context
                       ) throws IOException, InterruptedException {

HashMap<String, Integer> map =new HashMap<String, Integer>();
/*
 Dosent work if you try ot directly write to local files
PrintWriter writer = new PrintWriter("/home/hduser/Hadoop_learning_path/lession3/reducer.debug", "UTF-8");
*/
String outputPath="/home/hduser/Hadoop_learning_path/lession3/reducer.debug";
Path p = new Path(outputpath);
  Configuration conf = getConf();
 System.out.println("configured filesystem = " + conf.get(FS_PARAM_NAME));

FileSystem fs = FileSystem.get(conf);
        if (fs.exists(outputPath)) {
            System.err.println("output path exists");
}
OutputStream  os = fs.create(p);
// PrintWriter outWriter = new PrintWriter( new PrintStream(os));

for (Text text : values) {
    valuelist.add(text.toString());
}

// Add filenmaes to map as key and count as number
for (String val : valuelist) {

String file=val.toString();
if(map.get(file) !=null)
{
 Integer i=map.get(file);
    // remove and add new value
    map.remove(file);
     map.put(file,i+1); 
}
 else {
         map.put(file,1);
}
}

System.out.println("Hashmap contents");
String s = new String();
for(String i: map.keySet())
{
String k =i.toString();
            String value = map.get(i).toString();  
            s="<"+k + " " + value+">";

byte[] bytes =s.getBytes();
os.write(bytes);
os.flush();
}

// get collection keys and there values - standard hashmap iterate

Iterator<String> keySetIterator = map.keySet().iterator();

while(keySetIterator.hasNext()){
 String fNames = keySetIterator.next();
  op=op+" " + fNames + " : " + map.get(key);
}
result.set(op);
 context.write(key, result);
    }
  }

