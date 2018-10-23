// https://searchcode.com/api/result/12375312/

package org.jeffkubina.hadoop.utils.counting;

import java.io.IOException;
import java.util.*;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.*;
import org.apache.hadoop.mapred.*;

/**
 * The Class CountKeys.
 */
public class CountKeys
{
  /**
   * Instantiates a new count keys.
   * 
   * @param InputPath
   *          the input path
   * @param OutputPath
   *          the output path
   * @throws Exception
   *           the exception
   */
  public CountKeys(String InputPath, String OutputPath) throws Exception
  {
    countKeys(InputPath, OutputPath, 10);
  }

  /**
   * Count keys.
   * 
   * @param InputPath
   *          the input path
   * @param OutputPath
   *          the output path
   * @param BlockSize
   *          the block size
   * @throws Exception
   *           the exception
   */
  private void countKeys(String InputPath, String OutputPath, int BlockSize)
      throws Exception
  {
    /* configure the hadoop job to count the keys. */
    JobConf generateEdgesConf = new JobConf(CountKeys.class);

    /* set the number of unique keys to hold in memory. */
    generateEdgesConf.setInt("blockSize", BlockSize);

    /* set the hadoop parameter to keep the jvms alive. */
    generateEdgesConf.setNumTasksToExecutePerJvm(-1);

    /* set the name of the hadoop job via the graph characteristics. */
    generateEdgesConf.setJobName("countingKeys");

    /* set input format for the mapper class. */
    generateEdgesConf.setInputFormat(KeyValueTextInputFormat.class);

    /* set the mapper class to generate the pairs (key, partial count). */
    generateEdgesConf.setMapperClass(Mapper_CountKeys.class);
    generateEdgesConf.setMapOutputKeyClass(Text.class);
    generateEdgesConf.setMapOutputValueClass(LongWritable.class);

    /* set the reducer to generate the (key, total counts). */
    generateEdgesConf.setReducerClass(Reducer_CountKeys.class);
    generateEdgesConf.setOutputKeyClass(Text.class);
    generateEdgesConf.setOutputValueClass(LongWritable.class);

    /* set the output format for the reducer. */
    generateEdgesConf.setOutputFormat(TextOutputFormat.class);

    /* set the path the edge files are to be written to. */
    FileInputFormat.setInputPaths(generateEdgesConf, new Path(InputPath));
    FileOutputFormat.setOutputPath(generateEdgesConf, new Path(OutputPath));

    /* run the job (cross your fingers). */
    JobClient.runJob(generateEdgesConf);
  }

  /**
   * The Class Mapper_CountKeys.
   */
  public static class Mapper_CountKeys extends MapReduceBase implements
      Mapper<Text, Text, Text, LongWritable>
  {
    /** The block size. */
    private int blockSize;

    /** The key counts. */
    private HashMap<Text, long[]> keyCounts;

    /** The output collector. */
    private OutputCollector<Text, LongWritable> outputCollector;

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.hadoop.mapred.Mapper#map(java.lang.Object,
     * java.lang.Object, org.apache.hadoop.mapred.OutputCollector,
     * org.apache.hadoop.mapred.Reporter)
     */
    public void map(Text Key, Text Value,
        OutputCollector<Text, LongWritable> Output, Reporter reporter)
        throws IOException
    {
      /* store the output collector for the outputKeyCounts method. */
      if (outputCollector == null)
        outputCollector = Output;

      /* check if the key is new. */
      if (!keyCounts.containsKey(Key))
      {
        /* add the new key to the hashmap. */
        long count[] = new long[1];
        count[0] = 1;
        keyCounts.put(new Text(Key), count);

        /* if the number of keys in the hash is to large dump them all. */
        if (keyCounts.size() > blockSize)
          outputKeyCounts();
      } else
      {
        /* the key exists so increment its count. */
        keyCounts.get(Key)[0] += 1;
      }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.apache.hadoop.mapred.MapReduceBase#configure(org.apache.hadoop.mapred
     * .JobConf)
     */
    @Override
    public void configure(JobConf job)
    {
      /* set the max size for the keyCount hashmap. */
      blockSize = Math.max(1024, Math.abs(job.getInt("blockSize", 1024)));

      /* initialize the hash to hold the partial key counts. */
      keyCounts = new HashMap<Text, long[]>();

      /* clear the output collector. */
      outputCollector = null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.hadoop.mapred.MapReduceBase#close()
     */
    @Override
    public void close() throws IOException
    {
      outputKeyCounts();
    }

    /**
     * Outputs the key counts in the hashmap to the outputter.
     * 
     * @throws IOException
     *           Signals that an I/O exception has occurred.
     */
    private void outputKeyCounts() throws IOException
    {
      /* make text and long writables. */
      LongWritable countAsLw = new LongWritable();

      /* iterate through the keys. */
      Iterator<Text> keyIterator = keyCounts.keySet().iterator();
      while (keyIterator.hasNext())
      {
        /* get the key. */
        Text key = keyIterator.next();

        /* get the count. */
        long countOfKey[] = keyCounts.get(key);

        /* write the key and count to the output collector. */
        countAsLw.set(countOfKey[0]);
        outputCollector.collect(key, countAsLw);
      }

      /* clear all the data in the hash. */
      keyCounts.clear();
    }
  }

  /**
   * The Class Reducer_CountKeys.
   */
  public static class Reducer_CountKeys extends MapReduceBase implements
      Reducer<Text, LongWritable, Text, LongWritable>
  {
    /*
     * (non-Javadoc)
     * 
     * @see org.apache.hadoop.mapred.Reducer#reduce(java.lang.Object,
     * java.util.Iterator, org.apache.hadoop.mapred.OutputCollector,
     * org.apache.hadoop.mapred.Reporter)
     */
    public void reduce(Text Key, Iterator<LongWritable> Values,
        OutputCollector<Text, LongWritable> Output, Reporter reporter)
        throws IOException
    {
      /*
       * we could get the total jobs from the configuration parameters, but we
       * do this to ensures things are working as expected.
       */
      long sum = 0;
      while (Values.hasNext())
      {
        /* keep track of the total records read, should be just one. */
        sum += Values.next().get();
      }

      Output.collect(Key, new LongWritable(sum));
    }
  }
}

