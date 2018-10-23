// https://searchcode.com/api/result/12375311/

package org.jeffkubina.hadoop.utils.counting;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.*;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.*;
import org.apache.hadoop.mapred.*;
import org.apache.hadoop.io.SequenceFile;
import org.apache.hadoop.io.SequenceFile.CompressionType;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.mapred.FileOutputFormat;
import org.apache.hadoop.mapred.JobClient;
import org.apache.hadoop.mapred.OutputCollector;
import org.apache.hadoop.mapred.Reporter;

import org.jeffkubina.hadoop.utils.textpair.*;

/**
 * The Class CountKeys.
 */
public class DistributeSum
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
  public DistributeSum(String InputDirectory, String OutputDirectory,
      String TmpDirectory) throws Exception
  {
    /*
     * holds the temporary directory that intermediate results are stored in; it
     * is deleted when the computations are completed.
     */
    Path TmpDirectoryPath = new Path (TmpDirectory);

    /*
     * first mapreduce round stores the key-value pairs on the hdfs and computes
     * the partial sums.
     */
    Path keyJobIdPartialSumPath = new Path (TmpDirectoryPath,
        "keyJobIdPartialSum");
    Path jobIndexKeyValuePath = new Path (TmpDirectoryPath, "jobIndexKeyValue");
    distributedSumJob (InputDirectory, keyJobIdPartialSumPath,
        jobIndexKeyValuePath, 1L<<12);

    /* second mapreduce computes the total occurrence of the keys. */
    Path jobIndexKeySumPath = new Path (TmpDirectoryPath, "jobIndexKeySum");
    finalSumJob (keyJobIdPartialSumPath, jobIndexKeySumPath);

    /* third round merges the stored key-values and the occurrence sum. */
    Path outputDirectory = new Path (OutputDirectory);
    mergeKeyValueSum (jobIndexKeyValuePath, jobIndexKeySumPath, outputDirectory);

    /* remove all the temporary directories. */
  }

  /**
   * Count keys.
   * 
   * @param InputPath
   *          the input path
   * @param OutputPath
   *          the output path
   * @param pBlockSize
   *          the block size
   * @throws Exception
   *           the exception
   */
  private void distributedSumJob(String InputDirectory, Path KeyJobIdSumPath,
      Path JobIndexKeyValuePath, long pBlockSize) throws Exception
  {
    /* configure the hadoop job to count the keys. */
    JobConf job = new JobConf (DistributeSum.class);

    /* set number of jobs to partition the work over. */
    job.setLong ("blockSize", Math.max(1, Math.abs (pBlockSize)));

    /* set the hadoop parameter to keep the jvms alive. */
    job.setNumTasksToExecutePerJvm (-1);

    /* set the name of the hadoop job via the graph characteristics. */
    job.setJobName ("distributedSumJob");

    /* set input format for the mapper class. */
    job.setInputFormat (KeyValueTextInputFormat.class);

    /* set the mapper class to generate the pairs (key, partial count). */
    job.setMapperClass (DistributedSum_Mapper.class);
    job.setMapOutputKeyClass (TextPair.class);
    job.setMapOutputValueClass (Text.class);

    /* set how the keys are partitioned across jobs. */
    job.setPartitionerClass (DistributedSum_Partitioner.class);

    /* set the class to group the JobIndexKey by the job index only. */
    job.setOutputValueGroupingComparator (DistributedSum_GroupingComparator.class);

    /* set the class to sort the JobIndexKey by the job index and key. */
    job.setOutputKeyComparatorClass (DistributedSum_SortComparator.class);

    /* set the reducer to generate the (key, total counts). */
    job.setReducerClass (DistributedSum_Reducer.class);
    job.setOutputKeyClass (Text.class);
    job.setOutputValueClass (TextPair.class);

    /* set the output format for the reducer. */
    job.setOutputFormat (TextOutputFormat.class);

    /* set the path the edge files are to be written to. */
    FileInputFormat.setInputPaths (job, new Path (InputDirectory));
    FileOutputFormat.setOutputPath (job, KeyJobIdSumPath);

    /* create the temporary directory. */
    FileSystem hdfs = FileSystem.get (job);
    hdfs.mkdirs (JobIndexKeyValuePath);
    job.set ("jobIndexKeyValue", JobIndexKeyValuePath.toString ());

    /* run the job (cross your fingers). */
    JobClient.runJob (job);

    /* delete the temporary directory. */
    // hdfs.delete (new Path(jobIndexKeyValue), true);
  }

  /**
   * The Class Mapper_CountKeys.
   */
  public static class DistributedSum_Mapper extends MapReduceBase implements
      Mapper<Text, Text, TextPair, Text>
  {
    /** Hold the block size the summing is split over. */
    private long blockSize;

    /** Each record is mapped to a new job index. */
    private long jobIndex = 0;
    private Text jobIndexText;

    /**
     * Holds the write of the temporary file for the (jobIndex, (key, value))
     * pairs.
     */
    private String jobIdKeyValueFileName;
    private Path jobIdKeyValueWriterFilePath;
    // private SequenceFile.Writer jobIdKeyValueWriter;
    // private FSDataOutputStream jobIdKeyValueStream;

    private Writer jobIdKeyValueStream;

    /** Holds the temporary directory used to hold intermediate results. */
    private Path jobIndexKeyValue;

    /** Holds the file system for the job. */
    private FileSystem jobFileSystem;

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.hadoop.mapred.Mapper#map(java.lang.Object,
     * java.lang.Object, org.apache.hadoop.mapred.OutputCollector,
     * org.apache.hadoop.mapred.Reporter)
     */
    public void map(Text Key, Text Value,
        OutputCollector<TextPair, Text> Output, Reporter reporter)
        throws IOException
    {
      /* the job index cycles to zero when totalJobs is reached. */
      if (jobIndex >= blockSize)
        jobIndex = 0;

      /* convert the job index to text. */
      jobIndexText.set ("" + jobIndex);

      /* write the ((jobIndex, Key), Key) to the reducer. */
      Output.collect (new TextPair (jobIndexText, new Text (Key.toString ())),
          new Text (Key.toString ()));

      /* write the (jobIndex, (Key, Value)) to a file. */
      jobIdKeyValueStream.write ("t" + jobIndex + "\t" + Key.toString () + "\t" + Value.toString () + "\n");
      // jobIdKeyValueWriter.append (new Text ("t"+jobIndex), new TextPair (Key,
      // Value));

      /* increment to the next job index. */
      ++jobIndex;
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
      /* set total number of jobs to split the summing over. */
      blockSize = Math.abs (job.getLong ("blockSize", 1));

      /* initialize the job index. */
      jobIndex = 0;
      jobIndexText = new Text ();

      /* create the jobIdKeyValue writer. */
      jobIdKeyValueFileName = FileOutputFormat.getUniqueName (job,
          "jobIdKeyValue");
      jobIdKeyValueWriterFilePath = new Path (
          FileOutputFormat.getWorkOutputPath (job), jobIdKeyValueFileName);
      try
      {
        jobFileSystem = FileSystem.get (job);

        jobIdKeyValueStream = new BufferedWriter (new OutputStreamWriter (
            jobFileSystem.create (jobIdKeyValueWriterFilePath), "UTF-8"));

        // jobIdKeyValueStream = jobFileSystem.create
        // (jobIdKeyValueWriterFilePath);

        // jobIdKeyValueWriter = SequenceFile.createWriter (jobFileSystem, job,
        // jobIdKeyValueWriterFilePath, Text.class, TextPair.class,
        // CompressionType.NONE);
      } catch (IOException exception)
      {
        System.err.println ("IOException:" + exception.getMessage ());
      }

      /* set the temporary directory. */
      jobIndexKeyValue = new Path (job.get ("jobIndexKeyValue"));
    }

    @Override
    public void close() throws IOException
    {
      /* close writing to the jobIdKeyValue file. */
      // jobIdKeyValueWriter.close ();
      jobIdKeyValueStream.close ();

      /* move the jobIdKeyValue file to a separate directory. */
      jobFileSystem.rename (jobIdKeyValueWriterFilePath, new Path (
          jobIndexKeyValue, jobIdKeyValueFileName));
    }
  }

  /**
   * DistributedSum_Partitioner partitions only on the JobIndex of the
   * (jobIndex, key) pair. The Partitioner returns an integer from 0 to
   * NumberOfPartitions-1 that sets how the (jobIndex, key) pairs are
   * distributed across the processors.
   */
  public static class DistributedSum_Partitioner implements
      Partitioner<TextPair, Text>
  {
    @Override
    public int getPartition(TextPair JobIndexKey, Text Key,
        int NumberOfPartitions)
    {
      return Math.abs (JobIndexKey.hashCodeLeft ()) % NumberOfPartitions;
    }

    @Override
    public void configure(JobConf job)
    {
    }
  }

  /**
   * JobIndexKeySortComparator compares a pair of (jobIndex, key) so reduce jobs
   * receive the values sorted, which are the keys.
   */
  public static class DistributedSum_SortComparator implements
      RawComparator<TextPair>
  {
    @Override
    public int compare(byte[] b1, int s1, int l1, byte[] b2, int s2, int l2)
    {
      TextPair leftTextPair = new TextPair ();
      TextPair rightTextPair = new TextPair ();

      try
      {
        DataInputBuffer buffer = new DataInputBuffer ();
        buffer.reset (b1, s1, l1);
        leftTextPair.readFields (buffer);

        buffer.reset (b2, s2, l2);
        rightTextPair.readFields (buffer);
      } catch (IOException exception)
      {
        throw new RuntimeException (exception);
      }
      return compare (leftTextPair, rightTextPair);
    }

    @Override
    public int compare(TextPair LeftTextPair, TextPair RightTextPair)
    {
      return LeftTextPair.compareTo (RightTextPair);
    }
  }

  /**
   * DistributedSum_GroupingComparator compares only the jobIndex of a pair of
   * (jobIndex, key) so reduce jobs are given all JobIndexKey pairs with the
   * same jobIndex.
   */
  public static class DistributedSum_GroupingComparator implements
      RawComparator<TextPair>
  {
    @Override
    public int compare(byte[] b1, int s1, int l1, byte[] b2, int s2, int l2)
    {
      TextPair leftTextPair = new TextPair ();
      TextPair rightTextPair = new TextPair ();

      try
      {
        DataInputBuffer buffer = new DataInputBuffer ();
        buffer.reset (b1, s1, l1);
        leftTextPair.readFields (buffer);

        buffer.reset (b2, s2, l2);
        rightTextPair.readFields (buffer);
      } catch (IOException exception)
      {
        throw new RuntimeException (exception);
      }
      return compare (leftTextPair, rightTextPair);
    }

    @Override
    public int compare(TextPair LeftTextPair, TextPair RightTextPair)
    {
      return LeftTextPair.compareToLeft (RightTextPair);
    }
  }

  /**
   * The Class Reducer_CountKeys.
   */
  public static class DistributedSum_Reducer extends MapReduceBase implements
      Reducer<TextPair, Text, Text, TextPair>
  {
    /*
     * (non-Javadoc)
     * 
     * @see org.apache.hadoop.mapred.Reducer#reduce(java.lang.Object,
     * java.util.Iterator, org.apache.hadoop.mapred.OutputCollector,
     * org.apache.hadoop.mapred.Reporter)
     */
    public void reduce(TextPair JobIndexKey, Iterator<Text> Keys,
        OutputCollector<Text, TextPair> Output, Reporter reporter)
        throws IOException
    {
      // {
      // Text key = new Text ();
      // while (Keys.hasNext())
      // {
      // key.set (Keys.next());
      // Output.collect (key, new TextPair (JobIndexKey.getLeftText(), key));
      // }
      // }
      // if (true) return;

      /* should not really happen, but if there are no records, return now. */
      if (!Keys.hasNext ())
        return;

      /* get the job index. */
      Text JobIndex = new Text (JobIndexKey.getLeftText ());

      /*
       * the Keys are sorted, so we count them.
       */
      Text previousKey = new Text (Keys.next ());
      long keyCount = 1;

      /* count the keys and write their occurrence. */
      while (Keys.hasNext ())
      {

        Text currentKey = Keys.next ();
        int keyComparison = previousKey.compareTo (currentKey);
        if (keyComparison < 0)
        {
          /* write the new (key, keyCount) pairs to the output. */
          Output.collect (previousKey, new TextPair (JobIndex, new Text (
              keyCount + "")));

          /* store the new key and set it count to one. */
          previousKey.set (currentKey);
          keyCount = 1;
        } else if (keyComparison > 0)
        {
          /*
           * oh no, JobIndexKey are not sorted in ascending order, throw an
           * exception.
           */
          throw new RuntimeException (
              "JobIndexKey are not sorted in ascending order in the reducer.");
        } else
        {
          /* key occurred again, so increment its count. */
          ++keyCount;
        }
      }

      /* now write the pair (baseCompId, Vertex) to the vertex, compId file. */
      Output.collect (previousKey, new TextPair (JobIndex, new Text (keyCount
          + "")));
    }
  }

  private void finalSumJob(Path pKeyJobIdPartialSumPath, Path pKeyJobIdSumPath)
      throws Exception
  {
    /* configure the hadoop job to count the keys. */
    JobConf job = new JobConf (DistributeSum.class);

    /* set the hadoop parameter to keep the jvms alive. */
    job.setNumTasksToExecutePerJvm (-1);

    /* set the name of the hadoop job via the graph characteristics. */
    job.setJobName ("finalSumJob");

    /* set input format for the mapper class. */
    job.setInputFormat (KeyValueTextInputFormat.class);

    /* set the mapper class to generate the pairs (key, partial count). */
    job.setMapperClass (FinalSum_Mapper.class);
    job.setMapOutputKeyClass (Text.class);
    job.setMapOutputValueClass (Text.class);

    /* set the reducer to generate the (key, total counts). */
    job.setReducerClass (FinalSum_Reducer.class);
    job.setOutputKeyClass (Text.class);
    job.setOutputValueClass (TextPair.class);

    /* set the output format for the reducer. */
    job.setOutputFormat (TextOutputFormat.class);

    /* set the path the files are to be written to. */
    FileInputFormat.setInputPaths (job, pKeyJobIdPartialSumPath);
    FileOutputFormat.setOutputPath (job, pKeyJobIdSumPath);

    /* run the job (cross your fingers). */
    JobClient.runJob (job);
  }

  /**
   * The Class Mapper_CountKeys.
   */
  public static class FinalSum_Mapper extends MapReduceBase implements
      Mapper<Text, Text, Text, Text>
  {
    /*
     * (non-Javadoc)
     * 
     * @see org.apache.hadoop.mapred.Mapper#map(java.lang.Object,
     * java.lang.Object, org.apache.hadoop.mapred.OutputCollector,
     * org.apache.hadoop.mapred.Reporter)
     */
    public void map(Text Key, Text Value, OutputCollector<Text, Text> Output,
        Reporter reporter) throws IOException
    {
      /* write the (key, (jobId, partial sum)) to the reducer. */
      Output.collect (new Text (Key), new Text (Value));
    }
  }

  /**
   * The Class Reducer_CountKeys.
   */
  public static class FinalSum_Reducer extends MapReduceBase implements
      Reducer<Text, Text, Text, TextPair>
  {
    /** Holds the list of job indices processed by the reducer. */
    private ArrayDeque<String> queueOfJobIndices = new ArrayDeque<String> ();

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.hadoop.mapred.Reducer#reduce(java.lang.Object,
     * java.util.Iterator, org.apache.hadoop.mapred.OutputCollector,
     * org.apache.hadoop.mapred.Reporter)
     */
    public void reduce(Text Key, Iterator<Text> JobIndexPartialSums,
        OutputCollector<Text, TextPair> OutputCollector, Reporter reporter)
        throws IOException
    {
      /* should not really happen, but if there are no records, return now. */
      if (!JobIndexPartialSums.hasNext ())
        return;

      /* set total sum to zero; holds sum for a given key. */
      long totalSum = 0;

      /* clear the queue. */
      queueOfJobIndices.clear ();

      /* store the jobIndex and sum the counts. */
      while (JobIndexPartialSums.hasNext ())
      {
        /* split the value into the jobIndex and partial sum. */
        String jobIndexPartialSum[] = JobIndexPartialSums.next ().toString ()
            .split ("\t");

        /* if we did not get two values we have a serious bug. */
        if (jobIndexPartialSum.length != 2)
        {

        }

        /* store the jobIndex in the queue. */
        queueOfJobIndices.push (jobIndexPartialSum[0]);

        /* accumulate the occurrence of the key. */
        totalSum += Long.valueOf (jobIndexPartialSum[1]);
      }

      TextPair keySumPair = new TextPair (Key, new Text (totalSum + ""));
      Iterator<String> jobIndex = queueOfJobIndices.iterator ();
      while (jobIndex.hasNext ())
      {
        OutputCollector.collect (new Text ("s" + jobIndex.next ().toString ()),
            keySumPair);
      }
    }
  }

  private void mergeKeyValueSum(Path pJobIndexKeyValuePath,
      Path pJobIndexKeySumPath, Path pOutputDirectory) throws Exception
  {
    /* configure the hadoop job to merge the. */
    JobConf job = new JobConf (DistributeSum.class);

    /* set the hadoop parameter to keep the jvms alive. */
    job.setNumTasksToExecutePerJvm (-1);

    /* set the name of the hadoop job via the graph characteristics. */
    job.setJobName ("mergeKeyValueSumJob");

    /* set input format for the mapper class. */
    job.setInputFormat (KeyValueTextInputFormat.class);

    /* set the mapper class to generate the pairs (key, partial count). */
    job.setMapperClass (MergeKeyValueSum_Mapper.class);
    job.setMapOutputKeyClass (TextPair.class);
    job.setMapOutputValueClass (TextPair.class);

    /* set how the keys are partitioned across jobs. */
    job.setPartitionerClass (MergeKeyValueSum_Partitioner.class);

    /* set the class to sort the JobIndexKey by the job index and key. */
    job.setOutputKeyComparatorClass (MergeKeyValueSum_SortComparator.class);

    /* set the class to group the JobIndexKey by the job index only. */
    job.setOutputValueGroupingComparator (MergeKeyValueSum_GroupingComparator.class);

    /* set the reducer to generate the ((key, value), total counts). */
    job.setReducerClass (MergeKeyValueSum_Reducer.class);
    job.setOutputKeyClass (TextPair.class);
    job.setOutputValueClass (Text.class);

    /* set the output format for the reducer. */
    job.setOutputFormat (TextOutputFormat.class);

    /* set the path the files are to be written to. */
    FileInputFormat.setInputPaths (job, pJobIndexKeyValuePath,
        pJobIndexKeySumPath);
    FileOutputFormat.setOutputPath (job, pOutputDirectory);

    /* run the job (cross your fingers). */
    JobClient.runJob (job);
  }

  /**
   * The Class Mapper_CountKeys.
   */
  public static class MergeKeyValueSum_Mapper extends MapReduceBase implements
      Mapper<Text, Text, TextPair, TextPair>
  {
    /*
     * (non-Javadoc)
     * 
     * @see org.apache.hadoop.mapred.Mapper#map(java.lang.Object,
     * java.lang.Object, org.apache.hadoop.mapred.OutputCollector,
     * org.apache.hadoop.mapred.Reporter)
     */
    public void map(Text Key, Text Value,
        OutputCollector<TextPair, TextPair> Output, Reporter reporter)
        throws IOException
    {
      /* convert the value into a text pair. */
      String textPair[] = Value.toString ().split ("\t");

      /*
       * if the value did not parse into two strings, we have a programming
       * error.
       */
      if (textPair.length != 2)
      {

      }

      /* write the ((jobIndex, key), (key, value)) to the reducer. */
      Output.collect (new TextPair (Key.toString (), textPair[0]),
          new TextPair (textPair[0], textPair[1]));
    }

  }

  /**
   * MergeKeyValueSum_Partitioner partitions only on the JobIndex of the
   * (jobIndex, key) pair. The Partitioner returns an integer from 0 to
   * NumberOfPartitions-1 that sets how the (jobIndex, key) pairs are
   * distributed across the processors.
   */
  public static class MergeKeyValueSum_Partitioner implements
      Partitioner<TextPair, TextPair>
  {
    @Override
    public int getPartition(TextPair JobIndexKey, TextPair KeyValue,
        int NumberOfPartitions)
    {
      /*
       * disregard the first character of the jobIndex when computing the
       * partition number.
       */
      return Math.abs (JobIndexKey.getLeftText ().toString ().substring (1)
          .hashCode ())
          % NumberOfPartitions;
    }

    @Override
    public void configure(JobConf job)
    {
    }
  }

  /**
   * DistributedSum_GroupingComparator compares only the jobIndex of a pair of
   * (jobIndex, key) so reduce jobs are given all JobIndexKey pairs with the
   * same jobIndex.
   */
  public static class MergeKeyValueSum_GroupingComparator implements
      RawComparator<TextPair>
  {
    @Override
    public int compare(byte[] b1, int s1, int l1, byte[] b2, int s2, int l2)
    {
      TextPair leftTextPair = new TextPair ();
      TextPair rightTextPair = new TextPair ();

      try
      {
        DataInputBuffer buffer = new DataInputBuffer ();
        buffer.reset (b1, s1, l1);
        leftTextPair.readFields (buffer);

        buffer.reset (b2, s2, l2);
        rightTextPair.readFields (buffer);
      } catch (IOException exception)
      {
        throw new RuntimeException (exception);
      }
      return compare (leftTextPair, rightTextPair);
    }

    @Override
    public int compare(TextPair LeftTextPair, TextPair RightTextPair)
    {
      /* compare the left strings without their one character prefixes. */
      int comparison = LeftTextPair.getLeftText ().toString ().substring (1)
          .compareTo (RightTextPair.getLeftText ().toString ().substring (1));
      if (comparison != 0)
        return comparison;

      /* compare the right strings. */
      comparison = LeftTextPair.getRightText ().compareTo (
          RightTextPair.getRightText ());
        return comparison;
    }
  }

  /**
   * MergeKeyValueSum_SortComparator compares a pair of (jobIndex, key) so
   * reduce jobs receive the values sorted, which are the keys.
   */
  public static class MergeKeyValueSum_SortComparator implements
      RawComparator<TextPair>
  {
    @Override
    public int compare(byte[] b1, int s1, int l1, byte[] b2, int s2, int l2)
    {
      TextPair leftTextPair = new TextPair ();
      TextPair rightTextPair = new TextPair ();

      try
      {
        DataInputBuffer buffer = new DataInputBuffer ();
        buffer.reset (b1, s1, l1);
        leftTextPair.readFields (buffer);

        buffer.reset (b2, s2, l2);
        rightTextPair.readFields (buffer);
      } catch (IOException exception)
      {
        throw new RuntimeException (exception);
      }
      return compare (leftTextPair, rightTextPair);
    }

    @Override
    public int compare(TextPair LeftTextPair, TextPair RightTextPair)
    {
      /* compare the left strings without their one character prefixes. */
      int comparison = LeftTextPair.getLeftText ().toString ().substring (1)
          .compareTo (RightTextPair.getLeftText ().toString ().substring (1));
      if (comparison != 0)
        return comparison;

      /* compare the right strings. */
      comparison = LeftTextPair.getRightText ().compareTo (
          RightTextPair.getRightText ());
      if (comparison != 0)
        return comparison;

      /* compare the prefixes. */
      return LeftTextPair
          .getLeftText ()
          .toString ()
          .substring (0, 1)
          .compareTo (RightTextPair.getLeftText ().toString ().substring (0, 1));
    }
  }

  /**
   * The Class Reducer_CountKeys.
   */
  public static class MergeKeyValueSum_Reducer extends MapReduceBase implements
      Reducer<TextPair, TextPair, TextPair, Text>
  {
    /*
     * (non-Javadoc)
     * 
     * @see org.apache.hadoop.mapred.Reducer#reduce(java.lang.Object,
     * java.util.Iterator, org.apache.hadoop.mapred.OutputCollector,
     * org.apache.hadoop.mapred.Reporter)
     */
    public void reduce(TextPair JobIndexKey, Iterator<TextPair> Values,
        OutputCollector<TextPair, Text> OutputCollector, Reporter reporter)
        throws IOException
    {
      /* should not really happen, but if there are no records, return now. */
      if (!Values.hasNext ())
        return;

      /* the first value is the sum of the occurrences of the key. */
      Text sum = new Text (Values.next().getRightText ());

      /* store the ((key, value), sum) the counts. */
      while (Values.hasNext ())
      {
        OutputCollector.collect (new TextPair (Values.next ()), new Text (sum));
      }
    }
  }
}

