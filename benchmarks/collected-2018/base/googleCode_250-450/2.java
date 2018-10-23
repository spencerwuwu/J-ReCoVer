// https://searchcode.com/api/result/12375336/

package org.jeffkubina.graph.components;

import java.io.IOException;
import java.util.*;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.*;
import org.apache.hadoop.mapred.*;
import org.jeffkubina.hadoop.utils.textpair.TextPair;

/**
 * The Class CountKeys.
 */
public class MapOldCompIdToNewCompId
{
  /**
   * Instantiates a new count keys.
   * 
   * @param oldCompId_Vertex_OldCompIdOccurence_Path
   *          The path to the records (oldCompId, Vertex, OldCompIdOccurence).
   * @param oldCompId_NewCompId_OldCompIdOccurence_Path
   *          The path to the records (oldCompId, newCompId,
   *          OldCompIdOccurence). Each oldCompId must occur only once across
   *          all records.
   * @param newCompId_Vertex_Path
   *          The path where the records (newCompId, vertex) are written.
   * @throws Exception
   *           the exception
   */
  public MapOldCompIdToNewCompId(Path oldCompId_Vertex_OldCompIdOccurence_Path,
      Path oldCompId_NewCompId_OldCompIdOccurence_Path, long blockSize,
      Path newCompId_Vertex_Path) throws Exception
  {
    /* create the (0keyb, keya) records. */
    mapOldCompIdToNewCompId (oldCompId_Vertex_OldCompIdOccurence_Path,
        oldCompId_NewCompId_OldCompIdOccurence_Path, blockSize,
        newCompId_Vertex_Path);
  }

  private void mapOldCompIdToNewCompId(
      Path oldCompId_Vertex_OldCompIdOccurence_Path,
      Path oldCompId_NewCompId_OldCompIdOccurence_Path, long blockSize,
      Path newCompId_Vertex_Path) throws Exception
  {
    /* configure the hadoop job. */
    JobConf job = new JobConf (MapOldCompIdToNewCompId.class);

    /* set the hadoop parameter to keep the jvms alive. */
    job.setNumTasksToExecutePerJvm (-1);

    /* set the name of the hadoop job. */
    job.setJobName ("mapOldCompIdToNewCompId");

    job.set ("oldCompId_NewCompId_OldCompIdOccurence_Path",
        oldCompId_NewCompId_OldCompIdOccurence_Path.toString ());

    /*
     * set the mapper class to send (partitionIndex, (key, value)) to a file and
     * ((partitionIndex, key), key) to the reducer.
     */
    job.setMapperClass (MapOldCompIdToNewCompId_Mapper.class);
    /* set the mapper class to generate the pairs (key, partial count). */
    job.setMapperClass (MapOldCompIdToNewCompId_Mapper.class);
    job.setInputFormat (KeyValueTextInputFormat.class);
    job.setMapOutputKeyClass (Text.class);
    job.setMapOutputValueClass (Text.class);

    /* set how the keys are partitioned across jobs. */
    job.setPartitionerClass (MapOldCompIdToNewCompId_Partitioner.class);

    /*
     * set the class to sort the records so 0keyb and 1keyb are grouped together
     * with 0keyb first.
     */
    job.setOutputKeyComparatorClass (MapOldCompIdToNewCompId_SortComparator.class);

    /* set the class to group the JobIndexKey by the job index only. */
    job.setOutputValueGroupingComparator (MapOldCompIdToNewCompId_GroupingComparator.class);

    /* set the reducer to generate the ((key, value), total counts). */
    job.setReducerClass (MapOldCompIdToNewCompId_Reducer.class);
    job.setOutputKeyClass (Text.class);
    job.setOutputValueClass (TextPair.class);

    /* set the output format for the reducer. */
    job.setOutputFormat (TextOutputFormat.class);

    /* set the path the edge files are to be written to. */
    FileInputFormat.setInputPaths (job,
        oldCompId_Vertex_OldCompIdOccurence_Path,
        oldCompId_NewCompId_OldCompIdOccurence_Path);
    FileOutputFormat.setOutputPath (job, newCompId_Vertex_Path);

    /* run the job (cross your fingers). */
    JobClient.runJob (job);
  }

  /**
   * The Class Make0KeyaKeybRecords_Mapper.
   */
  public static class MapOldCompIdToNewCompId_Mapper extends MapReduceBase
      implements Mapper<Text, Text, Text, Text>
  {
    private String oldCompId_NewCompId_OldCompIdOccurence_Path;
    private long blockSize = 1000000;
    private Random random = new Random ();

    @Override
    public void configure(JobConf job)
    {
      oldCompId_NewCompId_OldCompIdOccurence_Path = job.get (
          "oldCompId_NewCompId_OldCompIdOccurence_Path", "");
      blockSize = job.getLong ("blockSize", 1000000);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.hadoop.mapred.Mapper#map(java.lang.Object,
     * java.lang.Object, org.apache.hadoop.mapred.OutputCollector,
     * org.apache.hadoop.mapred.Reporter)
     */
    public void map(Text key, Text value,
        OutputCollector<Text, Text> output, Reporter reporter)
        throws IOException
    {
      Path path = ((FileSplit) reporter.getInputSplit ()).getPath().getParent();
      String valueStr[] = value.toString().split ("\t");

      long occurrence = Long.valueOf (valueStr[1]);
      long totalBlocks = (occurrence + blockSize - 1) / blockSize;

      
      if (path.toString ().endsWith(oldCompId_NewCompId_OldCompIdOccurence_Path))
      {
        if (totalBlocks > 1)
        {
          for (long blockIndex = 0; blockIndex < totalBlocks; blockIndex++)
          {
            output.collect (new Text ("0" + key + "_" + blockIndex), new Text (valueStr[0]));
          }
        } else
        {
          output.collect (new Text ("0" + key + "_0"), new Text (valueStr[0]));
        }
      } else
      {
        if (totalBlocks > 1)
        {
          long blockIndex = random.nextLong () % totalBlocks;
          if (blockIndex < 0)
            blockIndex += totalBlocks;
          output.collect (new Text ("1" + key + "_" + blockIndex), new Text (valueStr[0]));
        } else
        {
          output.collect (new Text ("1" + key + "_0"), new Text (valueStr[0]));
        }
      }
    }
  }

  /**
   * MapOldCompIdToNewCompId_Partitioner partitions on the substring of the key
   * starting at index 1.
   */
  public static class MapOldCompIdToNewCompId_Partitioner implements
      Partitioner<Text, Text>
  {
    @Override
    public int getPartition(Text key, Text value, int numberOfPartitions)
    {
      long remainder = key.toString ().substring (1).hashCode ()
          % ((long) numberOfPartitions);
      if (remainder < 0)
        remainder += (long) numberOfPartitions;
      return (int) remainder;
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
  public static class MapOldCompIdToNewCompId_GroupingComparator implements
      RawComparator<Text>
  {
    @Override
    public int compare(byte[] b1, int s1, int l1, byte[] b2, int s2, int l2)
    {
      Text leftText = new Text ();
      Text rightText = new Text ();

      try
      {
        DataInputBuffer buffer = new DataInputBuffer ();
        buffer.reset (b1, s1, l1);
        leftText.readFields (buffer);

        buffer.reset (b2, s2, l2);
        rightText.readFields (buffer);
      } catch (IOException exception)
      {
        throw new RuntimeException (exception);
      }
      return compare (leftText, rightText);
    }

    @Override
    public int compare(Text leftText, Text rightText)
    {
      /* compare the strings without their one character prefixes. */
      int comparison = leftText.toString ().substring (1)
          .compareTo (rightText.toString ().substring (1));
      return comparison;
    }
  }

  /**
   * MergeKeyValueSum_SortComparator compares a pair of (jobIndex, key) so
   * reduce jobs receive the values sorted, which are the keys.
   */
  public static class MapOldCompIdToNewCompId_SortComparator implements
      RawComparator<Text>
  {
    @Override
    public int compare(byte[] b1, int s1, int l1, byte[] b2, int s2, int l2)
    {
      Text leftText = new Text ();
      Text rightText = new Text ();

      try
      {
        DataInputBuffer buffer = new DataInputBuffer ();
        buffer.reset (b1, s1, l1);
        leftText.readFields (buffer);

        buffer.reset (b2, s2, l2);
        rightText.readFields (buffer);
      } catch (IOException exception)
      {
        throw new RuntimeException (exception);
      }
      return compare (leftText, rightText);
    }

    @Override
    public int compare(Text leftText, Text rightText)
    {
      /* compare the strings without their one character prefixes. */
      int comparison = leftText.toString ().substring (1)
          .compareTo (rightText.toString ().substring (1));
      if (comparison != 0)
        return comparison;

      /* compare the prefixes. */
      return leftText.toString ().substring (0, 1)
          .compareTo (rightText.toString ().substring (0, 1));
    }
  }

  /**
   * The Class MakeKeybKeyaValuebRecords_Reducer.
   */
  public static class MapOldCompIdToNewCompId_Reducer extends MapReduceBase
      implements Reducer<Text, Text, Text, Text>
  {
    /*
     * (non-Javadoc)
     * 
     * @see org.apache.hadoop.mapred.Reducer#reduce(java.lang.Object,
     * java.util.Iterator, org.apache.hadoop.mapred.OutputCollector,
     * org.apache.hadoop.mapred.Reporter)
     */
    public void reduce(Text key, Iterator<Text> values,
        OutputCollector<Text, Text> outputCollector, Reporter reporter)
        throws IOException
    {
      /* should not really happen, but if there are no records, return now. */
      if (!values.hasNext ())
        return;

      Text newKey;
      if (key.toString ().startsWith ("1"))
      {
        /* if first key starts with 1 then the key remains the same. */
        String keyWithSuffix = key.toString().substring (1);
        int size = keyWithSuffix.lastIndexOf ("_");
        newKey = new Text (keyWithSuffix.substring (0, size));
      }
      else
      {
        /* if the first key starts with "0", then its value is the new key. */
        newKey = new Text (values.next ().toString ());
      }

      /* output (keyb, keya, valueb). */
      while (values.hasNext ())
      {
        outputCollector.collect (newKey, values.next ());
      }
    }
  }
}

