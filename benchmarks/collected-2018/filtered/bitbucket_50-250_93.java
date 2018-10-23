// https://searchcode.com/api/result/123156380/

/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fogbow.dicta13.hw1;

import java.io.IOException;
import java.util.*;
import fogbow.util.*;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.GenericOptionsParser;

public class DFcount {

  public static class TokenizerMapper extends Mapper<LongWritable, Text, Text, IntWritable>{
  
    private final static IntWritable one = new IntWritable(1);
    private Text outkey = new Text();
    
    public void map (LongWritable key, Text value, Context context) throws IOException, InterruptedException {
    
			HashSet<String> wordHash = new HashSet<String>();
      String[] sentenceParts = ((Text) value).toString().split("\\t+\\s*", 2);
      StringTokenizer itr = CleanStringTokenizer.apply(sentenceParts[1]);
      
      while (itr.hasMoreTokens ()){
        wordHash.add(itr.nextToken());
			}

			for (String word: wordHash){
				outkey.set(word);
				context.write(outkey, one);
			}
    }
  }
  
  public static class IntSumReducer extends Reducer<Text, IntWritable, Text, Text> {

    private Text result = new Text();

    public void reduce(Text key, Iterable<IntWritable> values, Context context)	throws IOException, InterruptedException {
      int sum = 0;
      int totalDocs = 4076*2;
      for (IntWritable val : values){
        sum += val.get();
      }
      Float idf=new Float(Math.log(totalDocs/sum));
      result.set(idf.toString());
      context.write(key, result);
    }
  }

  public static void main(String[] args) throws Exception {
    Configuration conf = new Configuration();
    Job job = new Job(conf, "DF count");
    job.setJarByClass(DFcount.class);
    job.setMapperClass(TokenizerMapper.class);
    //job.setCombinerClass(IntSumReducer.class);
    job.setReducerClass(IntSumReducer.class);
    job.setOutputKeyClass(Text.class);
    job.setOutputValueClass(IntWritable.class);
    FileInputFormat.addInputPath(job, new Path(args[0]));
    FileOutputFormat.setOutputPath(job, new Path(args[1]));
    System.exit(job.waitForCompletion(true) ? 0 : 1);
  }
}

