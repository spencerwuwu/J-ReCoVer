// https://searchcode.com/api/result/12375314/

package org.jeffkubina.hadoop.utils.counting;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.*;
import java.util.Random;

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

// TODO Add combiner to first mr-round.

/**
 * The Class CountKeys.
 */
public class AppendKeyOccurrence
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
  public AppendKeyOccurrence(String inputKeyValueDirectory, String outputKeyValueOccurrenceDirectory,
      String outputKeyOccurenceDirectory, long partitionSize, String tmpDirectory) throws Exception
  {
    /*
     * holds the temporary directory that intermediate results are stored in; it
     * is deleted when the computations are completed.
     */
    Path tmpDirectoryPath = new Path (tmpDirectory);

    /*
     * first mapreduce round stores the key-value pairs on the hdfs and computes
     * the partial sums.
     */
    Path keyPartitionIndexTallyPath = new Path (tmpDirectoryPath, "keyPartitionIndexTally");
    Path partitionIndexKeyValuePath = new Path (tmpDirectoryPath, "partitionIndexKeyValue");
    
    tallyPartitionedKeysRound (inputKeyValueDirectory, keyPartitionIndexTallyPath,
        partitionIndexKeyValuePath, partitionSize);

    /* second mapreduce computes the total occurrence of the keys. */
    Path partitionIndexKeyOccurrencePath = new Path (tmpDirectoryPath, "partitionIndexKeyTally");
    Path outputKeyOccurencePath = new Path (outputKeyOccurenceDirectory);
    sumPartitionedKeysRound (keyPartitionIndexTallyPath, partitionIndexKeyOccurrencePath, outputKeyOccurencePath);

    /* third round merges the stored key-values and the occurrence sum. */
    Path outputKeyValueOccurrencePath = new Path (outputKeyValueOccurrenceDirectory);
    mergeKeyValueSum (partitionIndexKeyValuePath, partitionIndexKeyOccurrencePath, outputKeyValueOccurrencePath);

    /* remove all the temporary directories. */
  }

  /**
   * Count keys.
   * 
   * @param inputKeyValueDirectory
   *          the input path
   * @param KeyJobIdSumPath
   *          the output path
   * @param partitionSize
   *          the block size
   * @throws Exception
   *           the exception
   */
  private void tallyPartitionedKeysRound (String inputKeyValueDirectory, Path KeyJobIdSumPath,
      Path partitionIndexKeyValuePath, long partitionSize) throws Exception
  {
    /* configure the hadoop job. */
    JobConf job = new JobConf (AppendKeyOccurrence.class);

    /* set number of jobs to partition the work over. */
    job.setLong ("partitionSize", Math.max(1, Math.abs (partitionSize)));

    /* set the hadoop parameter to keep the jvms alive. */
    job.setNumTasksToExecutePerJvm (-1);

    /* set the name of the hadoop job. */
    job.setJobName ("tallyPartitionedKeysRound");

    /* set the input format for the mapper class. */
    job.setInputFormat (KeyValueTextInputFormat.class);

    /* set the mapper class to send (partitionIndex, (key, value)) to a file and ((partitionIndex, key), key) to the reducer. */
    job.setMapperClass (TallyPartitionedKeys_Mapper.class);
    job.setMapOutputKeyClass (TextPair.class);
    job.setMapOutputValueClass (LongWritable.class);
    
    /* set the combiner class to generate the (partitionIndex, (key, tally)). */
    job.setCombinerClass (TallyPartitionedKeys_Combiner.class);
    
    /* set the reducer to generate the (key, total counts). */
    job.setReducerClass (TallyPartitionedKeys_Reducer.class);
    job.setOutputKeyClass (Text.class);
    job.setOutputValueClass (TextPair.class);

    /* set the output format for the reducer. */
    job.setOutputFormat (TextOutputFormat.class);

    /* set the path the edge files are to be written to. */
    FileInputFormat.setInputPaths (job, new Path (inputKeyValueDirectory));
    FileOutputFormat.setOutputPath (job, KeyJobIdSumPath);

    /* create the temporary directory. */
    FileSystem hdfs = FileSystem.get (job);
    hdfs.mkdirs (partitionIndexKeyValuePath);
    job.set ("partitionIndexKeyValueFileName", partitionIndexKeyValuePath.toString ());

    /* run the job (cross your fingers). */
    JobClient.runJob (job);

    /* delete the temporary directory. */
    // hdfs.delete (new Path(jobIndexKeyValue), true);
  }

  /**
   * The Class Mapper_CountKeys.
   */
  public static class TallyPartitionedKeys_Mapper extends MapReduceBase implements
      Mapper<Text, Text, TextPair, LongWritable>
  {
    /** Holds the partition size for the keys. */
    private long partitionSize;

    /** Hold the partition index each record is mapped to. */
    private Text partitionIndexText;
    private Random random = new Random ();

    /** Holds the info for the file of (partitionIndex, (key, value)) pairs. */
    private String partitionIndexKeyValueFileName;
    private Path partitionIndexKeyValueFilePath;
    private Path mappersPartitionIndexKeyValueFilePath;
    private Writer partitionIndexKeyValueWriter;

    /** Holds the file system for the job. */
    private FileSystem jobFileSystem;

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
      /* set the total number of partitions to split the keys over. */
      partitionSize = Math.abs (job.getLong ("partitionSize", 1));

      /* initialize the partition index. */
      partitionIndexText = new Text ();

      /* create the partitionIndexKeyValue output file writer to write the (partitionIndex, (key, value)) to. */
      partitionIndexKeyValueFileName = FileOutputFormat.getUniqueName (job,
          "partitionIndexKeyValue");
      mappersPartitionIndexKeyValueFilePath = new Path (
          FileOutputFormat.getWorkOutputPath (job), partitionIndexKeyValueFileName);
      try
      {
        /* get the jobs file system info. */
        jobFileSystem = FileSystem.get (job);

        /* create the writer for the mappersPartitionIndexKeyValueFilePath file. */
        partitionIndexKeyValueWriter = new BufferedWriter (new OutputStreamWriter (
            jobFileSystem.create (mappersPartitionIndexKeyValueFilePath), "UTF-8"));

      } catch (IOException exception)
      {
        System.err.println ("IOException:" + exception.getMessage ());
      }

      /* store the partitionIndexKeyValueFilePath for the close method. */
      partitionIndexKeyValueFilePath = new Path (job.get ("partitionIndexKeyValueFileName"));
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see org.apache.hadoop.mapred.Mapper#map(java.lang.Object,
     * java.lang.Object, org.apache.hadoop.mapred.OutputCollector,
     * org.apache.hadoop.mapred.Reporter)
     */
    public void map(Text key, Text value,
        OutputCollector<TextPair, LongWritable> output, Reporter reporter)
        throws IOException
    {
      /* the partition index cycles to zero when partitionSize is reached. */
    	long partitionIndex = Math.abs(random.nextLong()) % partitionSize;

      /* convert the partition index to text. */
      partitionIndexText.set ("" + partitionIndex);

      /* write the ((partitionIndex, key), 1) to the reducer. */
      output.collect (new TextPair (partitionIndexText, new Text (key)), new LongWritable (1));

      /* write the (jobIndex, (Key, Value)) to a file. */
      partitionIndexKeyValueWriter.write ("t" + partitionIndexText + "\t" + key.toString () + "\t" + value.toString () + "\n");
    }

    @Override
    public void close() throws IOException
    {
      /* close writing to the jobIdKeyValue file. */
      partitionIndexKeyValueWriter.close ();

      /* move the mappersPartitionIndexKeyValueFilePath file to the partitionIndexKeyValueFilePath directory. */
      jobFileSystem.rename (mappersPartitionIndexKeyValueFilePath, new Path (
          partitionIndexKeyValueFilePath, partitionIndexKeyValueFileName));
    }
  }

  
  /**
   * The Class TallyPartitionedKeys_Combiner.
   */
  public static class TallyPartitionedKeys_Combiner extends MapReduceBase implements
      Reducer<TextPair, LongWritable, TextPair, LongWritable>
  {
    /*
     * (non-Javadoc)
     * 
     * @see org.apache.hadoop.mapred.Reducer#reduce(java.lang.Object,
     * java.util.Iterator, org.apache.hadoop.mapred.OutputCollector,
     * org.apache.hadoop.mapred.Reporter)
     */
    public void reduce(TextPair partitionIndexKey, Iterator<LongWritable> ones,
        OutputCollector<TextPair, LongWritable> output, Reporter reporter)
        throws IOException
    {
      /* should not really happen, but if there are no records, return now. */
      if (!ones.hasNext ())
        return;

      /* tally the number of keys. */
      long keyTally = 0;
      while (ones.hasNext ())
      {
        ones.next ();
        ++keyTally;
      }

      /* write the pair (partitionIndexKey, tally). */
      output.collect (new TextPair (partitionIndexKey), new LongWritable (keyTally));
    }
  }

  
  /**
   * The Class Reducer_CountKeys.
   */
  public static class TallyPartitionedKeys_Reducer extends MapReduceBase implements
      Reducer<TextPair, LongWritable, Text, TextPair>
  {
    /*
     * (non-Javadoc)
     * 
     * @see org.apache.hadoop.mapred.Reducer#reduce(java.lang.Object,
     * java.util.Iterator, org.apache.hadoop.mapred.OutputCollector,
     * org.apache.hadoop.mapred.Reporter)
     */
    public void reduce(TextPair partitionIndexKey, Iterator<LongWritable> tallies,
        OutputCollector<Text, TextPair> output, Reporter reporter)
        throws IOException
    {
      /* should not really happen, but if there are no records, return now. */
      if (!tallies.hasNext ())
        return;

      /* get the job index. */
      Text partitionIndex = new Text (partitionIndexKey.getLeftText ());

      /* tally the number of keys. */
      Text previousKey = new Text (partitionIndexKey.getRightText ());
      long keyTally = 0;
      while (tallies.hasNext ())
      {
        keyTally += tallies.next().get();
      }

      /* write the pair (key, (partitionIndex, tally)). */
      output.collect (previousKey, new TextPair (partitionIndex, new Text (keyTally + "")));
    }
  }


  private void sumPartitionedKeysRound(Path keyPartitionIndexTallyPath, Path partitionIndexKeyOccurrencePath, Path outputKeyOccurencePath)
      throws Exception
  {
    /* configure the hadoop job. */
    JobConf job = new JobConf (AppendKeyOccurrence.class);

    /* set the hadoop parameter to keep the jvms alive. */
    job.setNumTasksToExecutePerJvm (-1);

    /* set the name of the hadoop job. */
    job.setJobName ("sumPartitionedKeysRound");

    /* set input format for the mapper class. */
    job.setInputFormat (KeyValueTextInputFormat.class);

    /* set the mapper class to generate the pairs (key, partial count). */
    job.setMapperClass (SumPartitionedKeys_Mapper.class);
    job.setMapOutputKeyClass (Text.class);
    job.setMapOutputValueClass (Text.class);

    /* set the reducer to generate the (partition index, (key, sum)). */
    job.setReducerClass (SumPartitionedKeys_Reducer.class);
    job.setOutputKeyClass (Text.class);
    job.setOutputValueClass (TextPair.class);

    /* set the output format for the reducer. */
    job.setOutputFormat (TextOutputFormat.class);

    /* set the path the files are to be written to. */
    FileInputFormat.setInputPaths (job, keyPartitionIndexTallyPath);
    FileOutputFormat.setOutputPath (job, partitionIndexKeyOccurrencePath);

    /* create the temporary directory. */
    FileSystem hdfs = FileSystem.get (job);
    hdfs.mkdirs (outputKeyOccurencePath);
    job.set ("keyOccurenceFilePath", outputKeyOccurencePath.toString ());

    /* run the job (cross your fingers). */
    JobClient.runJob (job);
    
    /* delete the keyPartitionIndexTallyPath directory since it is not needed anymore. */
    //hdfs.delete (keyPartitionIndexTallyPath);
  }

  /**
   * The Class SumPartitionedKeys_Mapper.
   */
  public static class SumPartitionedKeys_Mapper extends MapReduceBase implements
      Mapper<Text, Text, Text, Text>
  {
    /*
     * (non-Javadoc)
     * 
     * @see org.apache.hadoop.mapred.Mapper#map(java.lang.Object,
     * java.lang.Object, org.apache.hadoop.mapred.OutputCollector,
     * org.apache.hadoop.mapred.Reporter)
     */
    public void map(Text key, Text value, OutputCollector<Text, Text> output,
        Reporter reporter) throws IOException
    {
      /* write the (key, (jobId, partial sum)) to the reducer. */
      output.collect (new Text (key), new Text (value));
    }
  }

  /**
   * The Class SumPartitionedKeys_Reducer.
   */
  public static class SumPartitionedKeys_Reducer extends MapReduceBase implements
      Reducer<Text, Text, Text, TextPair>
  {
    /** Holds the list of job indices processed by the reducer. */
    private ArrayDeque<String> queueOfPartitionIndices = new ArrayDeque<String> ();

    /** Holds the info for the file of (partitionIndex, (key, value)) pairs. */
    private String keyOccurenceFileName;
    private Path keyOccurenceFilePath;
    private Path reducersKeyOccurenceFilePath;
    private Writer keyOccuranceWriter;

    /** Holds the file system for the job. */
    private FileSystem jobFileSystem;
    
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
      /* create the keyOccurence output file writer to write the (key, occurrence) to. */
      keyOccurenceFileName = FileOutputFormat.getUniqueName (job,
          "keyOccurence");
      reducersKeyOccurenceFilePath = new Path (
          FileOutputFormat.getWorkOutputPath (job), keyOccurenceFileName);
      try
      {
        /* get the jobs file system info. */
        jobFileSystem = FileSystem.get (job);

        /* create the writer for the mappersPartitionIndexKeyValueFilePath file. */
        keyOccuranceWriter = new BufferedWriter (new OutputStreamWriter (
            jobFileSystem.create (reducersKeyOccurenceFilePath), "UTF-8"));

      } catch (IOException exception)
      {
        System.err.println ("IOException:" + exception.getMessage ());
      }

      /* store the partitionIndexKeyValueFilePath for the close method. */
      keyOccurenceFilePath = new Path (job.get ("keyOccurenceFilePath"));
    }
    
    
    /*
     * (non-Javadoc)
     * 
     * @see org.apache.hadoop.mapred.Reducer#reduce(java.lang.Object,
     * java.util.Iterator, org.apache.hadoop.mapred.OutputCollector,
     * org.apache.hadoop.mapred.Reporter)
     */
    public void reduce(Text key, Iterator<Text> partitionIndexTally,
        OutputCollector<Text, TextPair> output, Reporter reporter)
        throws IOException
    {
      /* should not really happen, but if there are no records, return now. */
      if (!partitionIndexTally.hasNext ())
        return;

      /* clear the sum of the tally counts.. */
      long sum = 0;

      /* clear the queue. */
      queueOfPartitionIndices.clear ();

      /* store the partitionIndex and sum the tally values. */
      while (partitionIndexTally.hasNext ())
      {
        /* split the value into the partitionIndex and tally. */
        String partitionIndexTallyPair[] = partitionIndexTally.next ().toString ()
            .split ("\t");

        /* if we did not get two values we have a serious bug. */
        if (partitionIndexTallyPair.length != 2)
        {
          throw new RuntimeException ("In SumPartitionedKeys_Reducer.reduce record does not have two elements.");
        }

        /* store the partitionIndex in the queue. */
        queueOfPartitionIndices.push (partitionIndexTallyPair[0]);

        /* accumulate the occurrence of the key. */
        sum += Long.valueOf (partitionIndexTallyPair[1]);
      }

      /* write out (partitionIndex, (key, sum)) for each partitionIndex. */
      TextPair keySumPair = new TextPair (key, new Text (sum + ""));
      Iterator<String> partitionIndex = queueOfPartitionIndices.iterator ();
      while (partitionIndex.hasNext ())
      {
        output.collect (new Text ("s" + partitionIndex.next ().toString ()),
            keySumPair);
      }
      
      /* also write out the key sum pair. */
      keyOccuranceWriter.write (keySumPair.toString("\t") + "\n");
    }

    @Override
    public void close() throws IOException
    {
      /* close writing to the keyOccuranceWriter file. */
      keyOccuranceWriter.close ();

      /* move the reducersKeyOccurenceFilePath file to the keyOccurenceFilePath directory. */
      jobFileSystem.rename (reducersKeyOccurenceFilePath, new Path (
          keyOccurenceFilePath, keyOccurenceFileName));
    }  
  }

  private void mergeKeyValueSum(Path partitionIndexKeyValuePath,
      Path partitionIndexKeyOccurrencePath, Path outputKeyValueOccurrencePath) throws Exception
  {
    /* configure the hadoop job to merge the. */
    JobConf job = new JobConf (AppendKeyOccurrence.class);

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
    FileInputFormat.setInputPaths (job, partitionIndexKeyValuePath,
        partitionIndexKeyOccurrencePath);
    FileOutputFormat.setOutputPath (job, outputKeyValueOccurrencePath);

    /* run the job (cross your fingers). */
    JobClient.runJob (job);

    /* delete the partitionIndexKeyValuePath directory since it is not needed anymore. */
    FileSystem fileSystem = FileSystem.get (job);
    fileSystem.delete (partitionIndexKeyValuePath);
    fileSystem.delete (partitionIndexKeyOccurrencePath);
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
        throw new RuntimeException ("In MergeKeyValueSum_Mapper values does not have two elements.");
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
    public int getPartition(TextPair jobIndexKey, TextPair keyValue,
        int numberOfPartitions)
    {
      /*
       * disregard the first character of the jobIndex when computing the
       * partition number.
       */
      return Math.abs (jobIndexKey.getLeftText ().toString ().substring (1)
          .hashCode ())
          % numberOfPartitions;
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

