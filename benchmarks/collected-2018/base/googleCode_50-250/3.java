// https://searchcode.com/api/result/12375313/

package org.jeffkubina.hadoop.utils.counting;

import java.io.IOException;
import java.util.*;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.*;
import org.apache.hadoop.mapred.*;
import org.jeffkubina.hadoop.utils.textpair.*;

/**
 * The Class CountKeys.
 */
public class ConcatMatchingKeyToValueRecords
{
	/**
	 * Instantiates a new count keys.
	 * 
	 * @param inputKeyaKeybRecordsPath
	 *          The path to the records (Keya, Keyb).
	 * @param inputKeybValuebRecordsPath
	 *          The path to the records (Keyb, Valueb). Each key must occur only
	 *          once across all records.
	 * @param outputKeybKeyaValuebRecordsPath
	 *          The path where the records (Keyb, Keya, Valueb) are written.
	 * @throws Exception
	 *           the exception
	 */
	public ConcatMatchingKeyToValueRecords(Path inputKeyaKeybRecordsPath,
	    Path inputKeybValuebRecordsPath, Path outputKeybKeyaValuebRecordsPath)
	    throws Exception
	{
		/* create the records. */
		makeKeybKeyaValuebRecords(inputKeyaKeybRecordsPath,
		    inputKeybValuebRecordsPath, outputKeybKeyaValuebRecordsPath);
	}

	/**
	 * Map (Keya, Keyb) to (0Keyb, Keya).
	 * 
	 * @param inputKeyaKeybRecordsPath
	 *          The path to the records (Keya, Keyb).
	 * @param output1KeyaKeybRecordsPath
	 *          The path to the records (1Keya, Keyb).
	 * @throws Exception
	 *           the exception
	 */
	private void makeKeybKeyaValuebRecords(Path inputKeyaKeybRecordsPath,
	    Path inputKeybValuebRecordsPath, Path outputKeybKeyaValuebRecordsPath)
	    throws Exception
	{
		/* configure the hadoop job. */
		JobConf job = new JobConf(ConcatMatchingKeyToValueRecords.class);

		/* set the hadoop parameter to keep the jvms alive. */
		job.setNumTasksToExecutePerJvm(-1);

		/* set the name of the hadoop job. */
		job.setJobName("makeKeybKeyaValuebRecords");

		/* set the inputKeybValuebRecordsPath for the mapper. */
		job.set("inputKeybValuebRecordsPath", inputKeybValuebRecordsPath.toString());

		job.setInputFormat(KeyValueTextInputFormat.class);

    /* set the mapper class to generate the pairs (key, partial count). */
    job.setMapperClass (MakeKeybKeyaValuebRecords_Mapper.class);

		/* set the input format for the mapper class. */
    job.setInputFormat (KeyValueTextInputFormat.class);
    job.setMapOutputKeyClass (Text.class);
    job.setMapOutputValueClass (Text.class);

    /* set how the keys are partitioned across jobs. */
    job.setPartitionerClass (MakeKeybKeyaValuebRecords_Partitioner.class);

    /* set the class to group vy keyb. */
    job.setOutputValueGroupingComparator (MakeKeybKeyaValuebRecords_GroupingComparator.class);

    /* set the reducer to generate the (key, value, total counts). */
    job.setReducerClass (MakeKeybKeyaValuebRecords_Reducer.class);
    job.setOutputKeyClass (Text.class);
    job.setOutputValueClass (TextPair.class);

    /* set the output format for the reducer. */
    job.setOutputFormat (TextOutputFormat.class);

		/* set the path the edge files are to be written to. */
		FileInputFormat.setInputPaths(job, inputKeyaKeybRecordsPath,
		    inputKeybValuebRecordsPath);
		FileOutputFormat.setOutputPath(job, outputKeybKeyaValuebRecordsPath);

		/* run the job (cross your fingers). */
		JobClient.runJob(job);
	}

	/**
	 * The Class Make0KeyaKeybRecords_Mapper.
	 */
	public static class MakeKeybKeyaValuebRecords_Mapper extends MapReduceBase
	    implements Mapper<Text, Text, Text, Text>
	{
		private String inputKeybValuebRecordsPath;

		@Override
		public void configure(JobConf job)
		{
			inputKeybValuebRecordsPath = job.get("inputKeybValuebRecordsPath", "");
		}

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
			Path path = ((FileSplit) reporter.getInputSplit()).getPath().getParent();

			if (path.toString().endsWith(inputKeybValuebRecordsPath))
			{
				output.collect(new Text(key.toString() + "a"), value);
			} else
			{
				output.collect(new Text(value.toString() + "b"), key);
			}
		}
	}

	/**
	 * MergeKeyValueSum_Partitioner partitions only on the JobIndex of the
	 * (jobIndex, key) pair. The Partitioner returns an integer from 0 to
	 * NumberOfPartitions-1 that sets how the (jobIndex, key) pairs are
	 * distributed across the processors.
	 */
	public static class MakeKeybKeyaValuebRecords_Partitioner implements
	    Partitioner<Text, Text>
	{
		@Override
		public int getPartition(Text key, Text value, int numberOfPartitions)
		{
			long remainder = key.toString().substring(0, key.toString().length () - 1).hashCode()
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
	public static class MakeKeybKeyaValuebRecords_GroupingComparator implements
	    RawComparator<Text>
	{
		@Override
		public int compare(byte[] b1, int s1, int l1, byte[] b2, int s2, int l2)
		{
			Text leftText = new Text();
			Text rightText = new Text();

			try
			{
				DataInputBuffer buffer = new DataInputBuffer();
				buffer.reset(b1, s1, l1);
				leftText.readFields(buffer);

				buffer.reset(b2, s2, l2);
				rightText.readFields(buffer);
			} catch (IOException exception)
			{
				throw new RuntimeException(exception);
			}
			return compare(leftText, rightText);
		}

		@Override
		public int compare(Text leftText, Text rightText)
		{
			/* compare the strings without their one character prefixes. */
		  return leftText.toString().substring(0, leftText.toString().length ()-1)
			    .compareTo(rightText.toString().substring(0, rightText.toString ().length () - 1));
		}
	}

	/**
	 * The Class MakeKeybKeyaValuebRecords_Reducer.
	 */
	public static class MakeKeybKeyaValuebRecords_Reducer extends MapReduceBase
	    implements Reducer<Text, Text, Text, TextPair>
	{
		/*
		 * (non-Javadoc)
		 * 
		 * @see org.apache.hadoop.mapred.Reducer#reduce(java.lang.Object,
		 * java.util.Iterator, org.apache.hadoop.mapred.OutputCollector,
		 * org.apache.hadoop.mapred.Reporter)
		 */
		public void reduce(Text keyb, Iterator<Text> values,
		    OutputCollector<Text, TextPair> outputCollector, Reporter reporter)
		    throws IOException
		{
      /* should not really happen, but if there are no records, return now. */
			if (!values.hasNext())
				return;

			/* get keyb. */
			Text key = new Text(keyb.toString().substring(0, keyb.toString().length () - 1));

			/* if key is 1keyB, then there are no keya's. */
			Text valueb;
			if (keyb.toString().endsWith("b"))
				// valueb = new Text ("0");
				return;
			else
				valueb = new Text(values.next());

			/* output (keyb, keya, valueb). */
			while (values.hasNext())
			{
				outputCollector.collect(key, new TextPair(values.next(), valueb));
			}
		}
	}
}

