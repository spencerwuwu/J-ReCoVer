// https://searchcode.com/api/result/12375339/

package org.jeffkubina.graph.components;

import java.util.HashMap;
import java.util.Iterator;
import java.io.*;
import org.apache.hadoop.io.*;
import org.apache.hadoop.mapred.*;
import org.apache.hadoop.mapred.lib.MultipleOutputs;
import org.apache.hadoop.fs.*;
import org.apache.hadoop.conf.*;
import org.jeffkubina.graph.*;
import org.jeffkubina.hadoop.utils.textpair.*;

/**
 * The class ComputeConnectedComponentRound computes the connected components of
 * blocks of edges using Hadoop.
 * 
 * Given a directory of files containing edges, a mapper runs on sets of about
 * blockSize number of edges at a time, compute the connected components of the
 * subgraph comprising the edges.
 */
public class ComputeConnectedComponentRound
{
  private long blocksProcessed = 0;
  private static String compIdVertPrefix = "compIdVert";
  private long numberOfOutputRecords = 0;

  /**
   * Returns the number of edge blocks processed by all the mappers.
   * 
   * @returns Returns the number of edge blocks processed by all the mappers.
   */
  public long getBlocksProcessed()
  {
    return blocksProcessed;
  }
  
  public long getNumberOfOutputRecords()
  {
    return numberOfOutputRecords;
  }

  protected static enum BlockCounter {
    BLOCKS_PROCESSED
  };

  public int run(String[] Args) throws Exception
  {
    return 0;
  }

  /**
   * VertexCompIdPartitioner partitions only on the Vertex of the (vertex,
   * compId) pair. The Partitioner returns an integer from 0 to
   * NumberOfPartitions-1 that sets how the (vertex, compId) pairs are
   * distributed across the processors.
   */

  public static class VertexCompIdPartitioner implements
      Partitioner<TextPair, Text>
  {
    @Override
    public int getPartition(TextPair VertexCompId, Text CompId,
        int NumberOfPartitions)
    {
      long remainder = VertexCompId.hashCodeLeft()
          % ((long) NumberOfPartitions);
      if (remainder < 0)
        remainder += (long) NumberOfPartitions;
      return (int) remainder;
    }

    @Override
    public void configure(JobConf job)
    {
    }
  }

  /**
   * VertexCompIdSortComparator compares a pair of (vertex, compId) so reduce
   * jobs receive the values sorted, which are the compIds.
   */
  public static class VertexCompIdSortComparator implements
      RawComparator<TextPair>
  {
    @Override
    public int compare(byte[] b1, int s1, int l1, byte[] b2, int s2, int l2)
    {
      TextPair leftTextPair = new TextPair();
      TextPair rightTextPair = new TextPair();

      try
      {
        DataInputBuffer buffer = new DataInputBuffer();
        buffer.reset(b1, s1, l1);
        leftTextPair.readFields(buffer);

        buffer.reset(b2, s2, l2);
        rightTextPair.readFields(buffer);
      } catch (IOException exception)
      {
        throw new RuntimeException(exception);
      }
      return compare(leftTextPair, rightTextPair);
    }

    @Override
    public int compare(TextPair LeftTextPair, TextPair RightTextPair)
    {
      return LeftTextPair.compareTo(RightTextPair);
    }
  }

  /**
   * VertexCompIdGroupingComparator compares only the vertex of a pair of
   * (vertex, compId) so reduce jobs are given all VertexCompId pairs with the
   * same vertex.
   */
  public static class VertexCompIdGroupingComparator implements
      RawComparator<TextPair>
  {
    @Override
    public int compare(byte[] b1, int s1, int l1, byte[] b2, int s2, int l2)
    {
      TextPair leftTextPair = new TextPair();
      TextPair rightTextPair = new TextPair();

      try
      {
        DataInputBuffer buffer = new DataInputBuffer();
        buffer.reset(b1, s1, l1);
        leftTextPair.readFields(buffer);

        buffer.reset(b2, s2, l2);
        rightTextPair.readFields(buffer);
      } catch (IOException exception)
      {
        throw new RuntimeException(exception);
      }
      return compare(leftTextPair, rightTextPair);
    }

    @Override
    public int compare(TextPair LeftTextPair, TextPair RightTextPair)
    {
      return LeftTextPair.compareToLeft(RightTextPair);
    }
  }

  /**
   * Instantiates a new ComputeConnectedComponentRound that will generate the
   * graph.
   * 
   * @param PathToInputEdges
   * @param BlockSize
   * @param PathToOutputCompNodeEdges
   *          The list of command line arguments from the main program.
   * @throws Exception
   */

  public ComputeConnectedComponentRound(String pathToInputVertVertEdges,
      long BlockSize, String pathToOutputCompIdVertEdges, long numReduceTasks,
      long mapredMinSplitSize, 
      String pathToOutputCompCompEdges, boolean keepLoopEdges) throws Exception
  {
    /* set the job configuration parameters. */
    JobConf job = new JobConf(ComputeConnectedComponentRound.class);

    /* set the edge block size to process. */
    job.setLong("blockSize", Math.max(Math.abs(BlockSize), 2));
    job.setBoolean ("keepLoopEdges", keepLoopEdges);

    /* set the hadoop parameter to keep the jvms alive. */
    job.setNumTasksToExecutePerJvm(-1);
//    String jobPoolName = "";
//    job.set ("mapred.job.pool.name", jobPoolName);
//    job.set ("dfs.block.size", "268435456");
//    job.set ("io.sort.factor", "100");
//    job.set ("mapred.reduce.tasks.speculative.execution", "false");
//    job.set ("io.sort.record.percent", "0.20");
//    job.set ("io.file.buffer.size", "65536");
//    job.set ("io.sort.mb", "1024");
//    job.set ("mapred.child.java.opts", "-Xmx2G");
//    job.set ("mapred.output.compress", "false");
//    job.set ("mapred.output.compression.type", "BLOCK");
    //job.setNumReduceTasks (10);
    job.set ("mapred.min.split.size", mapredMinSplitSize + "");

    /* set the name of the hadoop job. */
    job.setJobName("computeConnectedComponentRound");

    // job.setInputFormatClass(KeyValueTextInputFormat.class);

    job.setMapperClass(Mapper_ComputeComponentsInBlocks.class);

    /*
     * set the mapping job class to read the edges and dump the (vertex, compId)
     * pairs.
     */
    job.setMapOutputKeyClass(TextPair.class);
    job.setMapOutputValueClass(Text.class);

    MultipleOutputs.addNamedOutput(job, compIdVertPrefix, TextOutputFormat.class,
        Text.class, Text.class);

    /* set the class to partition the VertexCompId by vertex only. */
    job.setPartitionerClass(VertexCompIdPartitioner.class);

    /* set the class to group the VertexCompId by vertex only. */
    job.setOutputValueGroupingComparator(VertexCompIdGroupingComparator.class);

    /* set the class to sort the VertexCompId by vertex and compId. */
    job.setOutputKeyComparatorClass(VertexCompIdSortComparator.class);

    /* set the combiner class to just remove duplicate (vertex, compId) pairs. */
    job.setCombinerClass(Combiner_ComputeComponentsInBlocks.class);

    /*
     * set the reducer class to write the (vertex, compId) and new (compId,
     * compId) pairs.
     */
    job.setReducerClass(Reducer_ComputeComponentsInBlocks.class);
    job.setOutputKeyClass(Text.class);
    job.setOutputValueClass(Text.class);
    job.setOutputFormat(TextOutputFormat.class);

    FileInputFormat.setInputPaths(job, new Path(pathToInputVertVertEdges));
    FileOutputFormat.setOutputPath(job, new Path(pathToOutputCompCompEdges));

    /* run the hadoop job. */
    RunningJob jobInfo = JobClient.runJob(job);

    /* set the number of blocks processed by the job. */
    blocksProcessed = jobInfo.getCounters().getCounter(
        BlockCounter.BLOCKS_PROCESSED);

    /* move the compIdVert files out of the default output directory. */
    moveCompIdVertFiles (pathToOutputCompCompEdges, pathToOutputCompIdVertEdges, compIdVertPrefix);
    
    /* set the total umber of output records. */
    numberOfOutputRecords = jobInfo.getCounters().findCounter("org.apache.hadoop.mapred.Task$Counter","REDUCE_OUTPUT_RECORDS").getCounter(); 
  }

  /**
   * Moves all the files in pathToOutputCompCompEdges with prefix compIdVert to
   * the directory pathToOutputCompIdVertEdges.
   */
  public void moveCompIdVertFiles(String inputPathAsString,
      String outputPathAsString, String filePrefix) throws Exception
  {
    /*
     * get the list of all the files in directory inputPathAsString with prefix
     * filePrefix.
     */
    try
    {
      /* get the default file system. */
      FileSystem fileSystem = FileSystem.get(new Configuration());
      
      /* make sure the directory the files are to be moved to exists. */
      fileSystem.mkdirs (new Path (outputPathAsString));    
      
      /* get the list of files in the directory. */
      Path inputPath = new Path (inputPathAsString);
      FileStatus[] listOfFileStatus = fileSystem.listStatus(inputPath);

      /* move each file that matches the prefix. */
      for (int i = 0; i < listOfFileStatus.length; i++)
      {
        /* get just the name of the file as a string. */
        String fileName = listOfFileStatus[i].getPath().getName();
        
        /* if the file name matches the prefix move it to outputPathAsString. */
        if (fileName.startsWith(filePrefix))
        {
          fileSystem.rename(listOfFileStatus[i].getPath(), new Path (outputPathAsString, fileName));
        }
      }
    } catch (Exception e)
    {
      System.out.println("File not found");
    }
  }

  /**
   * The class Mapper_ComputeComponentsInBlocks .
   */

  public static class Mapper_ComputeComponentsInBlocks extends MapReduceBase
      implements Mapper<Object, Text, TextPair, Text>
  {
    private long blockSize = 1024;
    private ComputeComponentsStreamingly streamingComponentCalculator = new ComputeComponentsStreamingly();
    private OutputCollector<TextPair, Text> output = null;
    private Reporter report = null;

    @Override
    public void configure(JobConf job)
    {
      blockSize = job.getLong("blockSize", 1024);
    }

    public void map(Object ignored, Text vertexFromTo,
        OutputCollector<TextPair, Text> outputCollector, Reporter reporter)
        throws IOException
    {
      if (output == null)
      {
        output = outputCollector;
        report = reporter;
      }

      /* add the edge to the component calculator. */
      String[] vertices = vertexFromTo.toString().split("\t");
      if (vertices.length > 1)
        streamingComponentCalculator.addEdge(vertices[0], vertices[1]);

      /*
       * if the number of vertices in the component calculator is too large,
       * dump the (vertex, compId).
       */
      if (streamingComponentCalculator.size() > blockSize)
      {
        /* get the list of (vertex, compId). */
        VertexCompId[] vertexCompIdList = streamingComponentCalculator
            .getVertexCompIdList();

        /* clear all the vertices in the component calculator. */
        streamingComponentCalculator.clear();

        /* write each of the (vertex, compId) pairs. */
        for (VertexCompId vertexCompId : vertexCompIdList)
        {
          outputCollector.collect(new TextPair(vertexCompId.getVertex(),
              vertexCompId.getCompId()), new Text(vertexCompId.getCompId()));
        }

        /* just in case ... */
        vertexCompIdList = null;

        /* increment the number of blocks processed. */
        reporter.incrCounter(BlockCounter.BLOCKS_PROCESSED, 1);
      }
    }

    /* dump any remaining (vertex, compId). */
    @Override
    public void close() throws IOException
    {
      /* get the list of (vertex, compId). */
      VertexCompId[] vertexCompIdList = streamingComponentCalculator
          .getVertexCompIdList();

      /* clear all the vertices in the component calculator. */
      streamingComponentCalculator.clear();

      /* write each of the (vertex, compId) pairs. */
      for (VertexCompId vertexCompId : vertexCompIdList)
      {
        output.collect(
            new TextPair(vertexCompId.getVertex(), vertexCompId.getCompId()),
            new Text(vertexCompId.getCompId()));
      }

      /* just in case ... */
      vertexCompIdList = null;

      /* increment the number of blocks processed. */
      report.incrCounter(BlockCounter.BLOCKS_PROCESSED, 1);
    }
  }

  /**
   * The class Combiner_ComputeComponentsInBlocks removes duplicate pairs; note
   * the pairs are assumed to be sorted by key and value.
   */
  public static class Combiner_ComputeComponentsInBlocks extends MapReduceBase
      implements Reducer<TextPair, Text, TextPair, Text>
  {
    /*
     * the combiner removes duplicate (Vertex, CompId) pairs; since the pairs
     * are sorted we need only compare successive pairs.
     */
    public void reduce(TextPair vertexCompId, Iterator<Text> compIds,
        OutputCollector<TextPair, Text> output, Reporter reporter)
        throws IOException
    {
      /* should not really happen, but if there are not records, return now. */
      if (!compIds.hasNext())
        return;

      /* used to compare successive compIds in CompIds. */
      Text previousCompId = new Text();

      while (compIds.hasNext())
      {
        Text compId = compIds.next();

        /* compare the current compId to the previous compId. */
        int comparison = previousCompId.compareTo(compId);

        /* if different, the previous will be smaller. */
        if (comparison < 0)
        {
          /* the compIds are different, so write the pair. */
          vertexCompId.setRightText(compId);
          output.collect(new TextPair (vertexCompId), new Text (compId));

          /* store the compId to compare it to the next one. */
          previousCompId.set(compId);
        } else if (comparison > 0)
        {
          /*
           * oh no, CompIds are not sorted in ascending order, throw an
           * exception.
           */

          throw new RuntimeException(
              "CompIds are not sorted in ascending order in the combiner.");
        }
      }
    }
  }

  public static class Reducer_ComputeComponentsInBlocks extends MapReduceBase
      implements Reducer<TextPair, Text, Text, Text>
  {
    private MultipleOutputs multipleOutputs;
    private OutputCollector<Text, Text> compIdvert;
    private HashMap<TextPair, long[]> newEdgeCache = new HashMap<TextPair, long[]>();
    private long blockSize;
    private boolean keepLoopEdges = true;
    
    private boolean isEdgeNew (Text leftVertex, Text rightVertex)
    {
      /* create the edge. */
      TextPair edge;
      
      /* normalize the edge. */
      if (leftVertex.compareTo (rightVertex) == -1)
      {
        edge = new TextPair (leftVertex, rightVertex);
      }
      else
      {
        edge = new TextPair (rightVertex, leftVertex);
      }
      
      /* if the edge is in the cache return false. */
      if (newEdgeCache.containsKey(edge)) return false;
      
      /* need to add the edge to the cache. */
      
      /* clear the cache if too large. */
      if (newEdgeCache.size() >= blockSize) newEdgeCache.clear();
      
      /* add the new edge. */
      newEdgeCache.put(edge, null);
      
      return true;
    }

    @Override
    public void configure(JobConf job)
    {
      multipleOutputs = new MultipleOutputs(job);
      blockSize = job.getLong("blockSize", 1024);
      keepLoopEdges = job.getBoolean ("keepLoopEdges", true);

    }

    public void reduce(TextPair vertex, Iterator<Text> compIds,
        OutputCollector<Text, Text> output, Reporter reporter)
        throws IOException
    {
      if (compIdvert == null)
        compIdvert = multipleOutputs.getCollector(compIdVertPrefix, reporter);

      /* should not really happen, but if there are not records, return now. */
      if (!compIds.hasNext())
        return;

      /*
       * the values are sorted, so the base id of the vertex is the first
       * compId.
       */
      Text baseCompId = new Text(compIds.next());
      Text previousCompId = new Text(baseCompId);

      /* write the newly generated (compId, compId) pairs to the output. */
      while (compIds.hasNext())
      {
        Text compId = compIds.next();
        int compIdComparison = previousCompId.compareTo(compId);
        if (compIdComparison < 0)
        {
          /* write the new (compId, compId) pairs to the output. */
          if (isEdgeNew (baseCompId, compId))
          {
            /* if the edge is not a loop and we are not skipping them, write the edge. */
            if (keepLoopEdges || (baseCompId.compareTo (compId) != 0))
            {
              output.collect(baseCompId, compId);
              //output.collect(compId, baseCompId);
            }
          }

          /* store the new compId. */
          previousCompId.set(compId);
        } else if (compIdComparison > 0)
        {
          /*
           * oh no, CompIds are not sorted in ascending order, throw an
           * exception.
           */
          throw new RuntimeException(
              "CompIds are not sorted in ascending order in the reducer.");
        }
      }

      /* now write the pair (baseCompId, Vertex) to the vertex, compId file. */
      if (keepLoopEdges || (baseCompId.compareTo (vertex.getLeftText()) != 0))
      {
        compIdvert.collect(baseCompId, vertex.getLeftText());
      }
    }

    public void close() throws IOException
    {
      multipleOutputs.close();
      newEdgeCache = null;
    }
  }
}

