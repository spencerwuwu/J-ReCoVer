// https://searchcode.com/api/result/65662153/

/*
 * Cloud9: A MapReduce Library for Hadoop
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you
 * may not use this file except in compliance with the License. You may
 * obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package edu.umd.cloud9.collection.trec;

import java.io.IOException;
import java.util.Iterator;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.FileInputFormat;
import org.apache.hadoop.mapred.FileOutputFormat;
import org.apache.hadoop.mapred.JobClient;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.MapReduceBase;
import org.apache.hadoop.mapred.Mapper;
import org.apache.hadoop.mapred.OutputCollector;
import org.apache.hadoop.mapred.Reducer;
import org.apache.hadoop.mapred.Reporter;
import org.apache.hadoop.mapred.TextOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.apache.log4j.Logger;

/**
 * <p>
 * Program that builds the mapping from TREC docids (String identifiers) to docnos
 * (sequentially-numbered ints). The program takes four command-line arguments:
 * </p>
 *
 * <ul>
 * <li>[input] path to the document collection</li>
 * <li>[output-dir] path to temporary MapReduce output directory</li>
 * <li>[output-file] path to location of mappings file</li>
 * </ul>
 *
 * <p>
 * Here's a sample invocation:
 * </p>
 *
 * <blockquote><pre>
 * setenv HADOOP_CLASSPATH "/foo/cloud9-x.y.z.jar:/foo/guava-r09.jar"
 *
 * hadoop jar cloud9-x.y.z.jar edu.umd.cloud9.collection.trec.NumberTrecDocuments \
 *   -libjars=guava-r09.jar \
 *   /shared/collections/trec/trec4-5_noCRFR.xml \
 *   /user/jimmylin/trec-docid-tmp \
 *   /user/jimmylin/docno-mapping.dat
 * </pre></blockquote>
 *
 * @author Jimmy Lin
 */
@SuppressWarnings("deprecation")
public class NumberTrecDocuments extends Configured implements Tool {
  private static final Logger LOG = Logger.getLogger(NumberTrecDocuments.class);
  private static enum Count { DOCS };

  private static class MyMapper extends MapReduceBase implements
      Mapper<LongWritable, TrecDocument, Text, IntWritable> {
    private static final Text docid = new Text();
    private static final IntWritable one = new IntWritable(1);

    public void map(LongWritable key, TrecDocument doc, OutputCollector<Text, IntWritable> output,
        Reporter reporter) throws IOException {
      reporter.incrCounter(Count.DOCS, 1);

      docid.set(doc.getDocid());
      output.collect(docid, one);
    }
  }

  private static class MyReducer extends MapReduceBase implements
      Reducer<Text, IntWritable, Text, IntWritable> {
    private static final IntWritable cnt = new IntWritable(1);

    public void reduce(Text key, Iterator<IntWritable> values,
        OutputCollector<Text, IntWritable> output, Reporter reporter) throws IOException {
      output.collect(key, cnt);
      cnt.set(cnt.get() + 1);
    }
  }

  /**
   * Creates an instance of this tool.
   */
  public NumberTrecDocuments() {
  }

  private static int printUsage() {
    System.out.println("usage: [input-path] [output-path] [output-file]");
    ToolRunner.printGenericCommandUsage(System.out);
    return -1;
  }

  /**
   * Runs this tool.
   */
  public int run(String[] args) throws Exception {
    if (args.length != 3) {
      printUsage();
      return -1;
    }

    String inputPath = args[0];
    String outputPath = args[1];
    String outputFile = args[2];

    LOG.info("Tool: " + NumberTrecDocuments.class.getCanonicalName());
    LOG.info(" - Input path: " + inputPath);
    LOG.info(" - Output path: " + outputPath);
    LOG.info(" - Output file: " + outputFile);

    JobConf conf = new JobConf(getConf(), NumberTrecDocuments.class);
    conf.setJobName(NumberTrecDocuments.class.getSimpleName());

    conf.setNumMapTasks(100); // Arbitrary number; doesn't matter.
    conf.setNumReduceTasks(1);

    FileInputFormat.setInputPaths(conf, new Path(inputPath));
    FileOutputFormat.setOutputPath(conf, new Path(outputPath));
    FileOutputFormat.setCompressOutput(conf, false);

    conf.setInputFormat(TrecDocumentInputFormat.class);
    conf.setOutputKeyClass(Text.class);
    conf.setOutputValueClass(IntWritable.class);
    conf.setOutputFormat(TextOutputFormat.class);

    conf.setMapperClass(MyMapper.class);
    conf.setReducerClass(MyReducer.class);

    // delete the output directory if it exists already
    FileSystem.get(conf).delete(new Path(outputPath), true);

    JobClient.runJob(conf);

    String input = outputPath + (outputPath.endsWith("/") ? "" : "/") + "/part-00000";
    TrecDocnoMapping.writeMappingData(new Path(input), new Path(outputFile),
        FileSystem.get(getConf()));

    return 0;
  }

  /**
   * Dispatches command-line arguments to the tool via the {@code ToolRunner}.
   */
  public static void main(String[] args) throws Exception {
    ToolRunner.run(new Configuration(), new NumberTrecDocuments(), args);
  }
}

