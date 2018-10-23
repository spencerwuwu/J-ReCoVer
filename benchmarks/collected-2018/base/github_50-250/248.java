// https://searchcode.com/api/result/73507140/

/*
 * Copyright (C) 2003-2011 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.sample.hadoop;

import java.io.IOException;
import java.util.StringTokenizer;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.GenericOptionsParser;



/**
 * @author <a href="mailto:haint@exoplatform.com">Nguyen Thanh Hai</a>
 *
 * @datAug 31, 2011
 */
public class WordCount
{
   public static class TokenizerMapper extends Mapper<Object, Text, Text, IntWritable>{
      private IntWritable one = new IntWritable(1);
      private Text word = new Text();
      
      public void map(Object key, Text value, Context context) throws IOException, InterruptedException
      {
         StringTokenizer tokenizer = new StringTokenizer(value.toString());
         while(tokenizer.hasMoreTokens()) {
            word.set(tokenizer.nextToken());
            context.write(word, one);
         }
      }
   }
   
   public static class IntSumReducer extends Reducer<Text, IntWritable, Text, IntWritable> {
      private IntWritable result = new IntWritable();
      
      public void reduce(Text key, Iterable<IntWritable> values, Context context) throws IOException, InterruptedException 
      {
         int sum = 0; 
         for(IntWritable value : values) {
            sum += value.get();
         }
         result.set(sum);
         context.write(key, result);
      }
   }
   
   public static void main(String[] args) throws Exception {
      Configuration configuration = new Configuration();
      String[] otherArgs = new GenericOptionsParser(configuration, args).getRemainingArgs();
      if(otherArgs.length != 2) {
         System.err.println("Usage: wordcount <in> <out>");
         System.exit(2);
      }
      
      Job job = new Job();
      job.setJarByClass(WordCount.class);
      job.setJobName(WordCount.class.getSimpleName());
      job.setCombinerClass(IntSumReducer.class);
      job.setMapperClass(TokenizerMapper.class);
      job.setOutputKeyClass(Text.class);
      job.setOutputValueClass(IntWritable.class);
      FileInputFormat.addInputPath(job, new Path(otherArgs[0]));
      FileOutputFormat.setOutputPath(job, new Path(otherArgs[1]));
      System.exit(job.waitForCompletion(true) ? 0 : 1);
   }
}

