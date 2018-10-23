// https://searchcode.com/api/result/12375332/

package org.jeffkubina.graph.generate.random;

import java.io.IOException;
import java.util.*;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.*;
import org.apache.hadoop.mapred.*;
import org.jeffkubina.hadoop.utils.*;
import org.jeffkubina.graph.*;
import org.apache.hadoop.io.compress.*;

/**
 * The class GenerateGraphUsingHadoop generates the graph of edge files and
 * vertex component-id files using Hadoop jobs for parallelism.
 */
public class GenerateGraphUsingHadoop
{
	/**
	 * Instantiates a new GenerateGraphUsingThreads that will generate the graph.
	 * 
	 * @param CommandLineArguments
	 *          The list of command line arguments from the main program.
	 * @throws Exception
	 */
	public GenerateGraphUsingHadoop(String[] CommandLineArguments)
	    throws Exception
	{
		/* parse the command line options; need them to get the random seed. */
		CommandLineParameters commandLineParameters = new CommandLineParameters(
		    CommandLineArguments);

		/* get the random seed. */
		long randomSeed = commandLineParameters.get_randomSeed();

		/* generate the graph edges. */
		generateGraphEdges(CommandLineArguments, randomSeed);

		/* generate the vertex component-id pairs. */
		generateGraphVertexCompIds(CommandLineArguments, randomSeed);
	}

	/**
	 * Generates the edges of the graph.
	 * 
	 * @param CommandLineArguments
	 *          The list of command line arguments from the main program.
	 * @param RandomSeed
	 *          The random seed to use.
	 * @throws Exception
	 */
	private void generateGraphEdges(String[] CommandLineArguments, long RandomSeed)
	    throws Exception
	{
		/* process the command line parameters. */
		CommandLineParameters commandLineParameters = new CommandLineParameters(
		    CommandLineArguments);

		/* configure the hadoop job to generate the edges. */
		JobConf job = new JobConf(GenerateGraphUsingHadoop.class);

		/* set all the graph generation parameters for the mapper and reducer. */
		job.setLong("totalNodes",
		    commandLineParameters.get_vertices());
		job.setLong("totalEdges", commandLineParameters.get_edges());
		job.setLong("totalComponents",
		    commandLineParameters.get_components());
		job.setInt("totalJobs",
		    (int) commandLineParameters.get_processes());
		job.setLong("randomSeed", RandomSeed);
		job.set("delimiter", commandLineParameters.get_delimiter());
		job
		    .setFloat("skew", (float) commandLineParameters.get_skew());
		job.set("map.output.key.field.separator",
		    commandLineParameters.get_delimiter());
		job.setBoolean ("sequencefile", commandLineParameters.get_sequencefile());
		
		/* set the partition probabilities. */
		double[] probabilities = commandLineParameters.get_probabilities();
		for (int i = 0; i < 4; i++)
		{
			job.setFloat("probabilities[" + i + "]",
			    (float) probabilities[i]);
		}

		/* get the integer used to permute the vertex identifiers. */
		long permuter = 0;
		if (commandLineParameters.get_randomize())
		{
			Random random = new Random(RandomSeed);
			permuter = commandLineParameters.get_vertices()
			    ^ Math.abs((Math.abs(random.nextLong()) % commandLineParameters
			        .get_vertices()));
		}
		job.setLong("permuter", permuter);

		/* set the hadoop parameter to keep the jvms alive. */
		job.setNumTasksToExecutePerJvm(-1);

		/* set the number of reduce tasks to the number of jobs. */
		job.setNumReduceTasks((int) commandLineParameters
		    .get_processes());

		/* set the name of the hadoop job via the graph characteristics. */
		job.setJobName("gengraph-edges-e"
		    + commandLineParameters.get_edges() + "-v"
		    + commandLineParameters.get_vertices() + "-c"
		    + commandLineParameters.get_components() + "-s"
		    + commandLineParameters.get_randomSeed());

		/* set input format for the mapper class. */
		job.setInputFormat(OnlyOneInputFormat.class);

		/* set the mapper class to generate the pairs (jobId, totalJobs). */
		job.setMapperClass(Mapper_MakeJobIdTotalJobsPairs.class);

		/* set the reducer to generate the edges. */
		job.setReducerClass(Reducer_GenerateEdges.class);
		
		/* set the output key and value classes. */
		job.setOutputKeyClass(LongWritable.class);
		job.setOutputValueClass(LongWritable.class);
		
		/* set the output file type via the command line parameter sequencefile. */
		if (commandLineParameters.get_sequencefile())
		{
		  job.setOutputFormat(SequenceFileOutputFormat.class);
	    FileOutputFormat.setCompressOutput (job, true);
	    FileOutputFormat.setOutputCompressorClass(job, GzipCodec.class);
		}
		else
		  job.setOutputFormat(TextOutputFormat.class);
		
		/* set the job timeout longer since the generation of the edges is cpu intensive. */
		{
		  long timeout = 24 * 60 * 60 * 1000;
		  job.set("mapred.task.timeout", "" + timeout);
		}
		
		/* set the path the edge files are to be written to. */
		FileOutputFormat.setOutputPath(job, new Path(
		    commandLineParameters.get_pathToEdges()));

		/* run the job (cross your fingers). */
		JobClient.runJob(job);
	}

	/**
	 * Generates the vertex and component-id pairs of the graph.
	 * 
	 * @param CommandLineArguments
	 *          The list of command line arguments from the main program.
	 * @param RandomSeed
	 *          The random seed to use.
	 */
	private void generateGraphVertexCompIds(String[] CommandLineArguments,
	    long RandomSeed) throws Exception
	{
		/* process the command line parameters. */
		CommandLineParameters commandLineParameters = new CommandLineParameters(
		    CommandLineArguments);

		/* configure the hadoop job to generate the vertex component-id pairs. */
		JobConf job = new JobConf(
		    GenerateGraphUsingHadoop.class);

		/* set all the vertex component-id pairs generation parameters. */
		job.setLong("totalNodes",
		    commandLineParameters.get_vertices());
		job.setLong("totalComponents",
		    commandLineParameters.get_components());
		job.setInt("totalJobs",
		    (int) commandLineParameters.get_processes());
		job.setLong("randomSeed", RandomSeed);
		job.set("delimiter",
		    commandLineParameters.get_delimiter());
		job.setFloat("skew",
		    (float) commandLineParameters.get_skew());
		job.set("map.output.key.field.separator",
		    commandLineParameters.get_delimiter());

		/* get the integer used to permute the vertex identifiers. */
		long permuter = 0;
		if (commandLineParameters.get_randomize())
		{
			Random random = new Random(RandomSeed);
			permuter = commandLineParameters.get_vertices()
			    ^ Math.abs((Math.abs(random.nextLong()) % commandLineParameters
			        .get_vertices()));
		}
		job.setLong("permuter", permuter);

		/* set the hadoop parameters to keep the jvms alive. */
		job.setNumTasksToExecutePerJvm(-1);

		/* set the number of reduce tasks to the number of jobs. */
		job.setNumReduceTasks((int) commandLineParameters
		    .get_processes());
    
		/* set the time out longer since the job is cpu intensive. */
		{
      long timeout = 24 * 60 * 60 * 1000;
      job.set("mapred.task.timeout", "" + timeout);
    }
    
		/* set the name of the hadoop job via the graph characteristics. */
		job.setJobName("gengraph-vertexCompIds-v"
		    + commandLineParameters.get_edges() + "-e"
		    + commandLineParameters.get_edges() + "-c"
		    + commandLineParameters.get_components() + "-s"
		    + commandLineParameters.get_randomSeed());

		/* set the input format for the mapper class. */
		job.setInputFormat(OnlyOneInputFormat.class);

		/* set the mapper class to generate the pairs (jobId, totalJobs). */
		job
		    .setMapperClass(Mapper_MakeJobIdTotalJobsPairs.class);

		/* set the reducer to generate the vertex component-id pairs. */
		job
		    .setReducerClass(Reducer_GenerateVertexCompIds.class);
		job.setOutputKeyClass(LongWritable.class);
		job.setOutputValueClass(LongWritable.class);
		job.setOutputFormat(TextOutputFormat.class);

		/* set the path the vertex component-id pairs are to be written to. */
		FileOutputFormat.setOutputPath(job, new Path(
		    commandLineParameters.get_pathToVertexComponentId()));

		/* run the job (cross your fingers). */
		JobClient.runJob(job);
	}

	/**
	 * The class Mapper_MakeJobIdTotalJobsPairs generates totalJobs pairs of the
	 * form (jobId, totalJobs where jobId ranges from 0 to totalJobs - 1.
	 */
	public static class Mapper_MakeJobIdTotalJobsPairs extends MapReduceBase
	    implements Mapper<LongWritable, LongWritable, LongWritable, LongWritable>
	{

		/** The total jobs. */
		private long totalJobs;

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.apache.hadoop.mapred.Mapper#map(java.lang.Object,
		 * java.lang.Object, org.apache.hadoop.mapred.OutputCollector,
		 * org.apache.hadoop.mapred.Reporter)
		 */
		public void map(LongWritable IgnoredKey, LongWritable IgnoredValue,
		    OutputCollector<LongWritable, LongWritable> output, Reporter reporter)
		    throws IOException
		{
			LongWritable lwJobId = new LongWritable();
			LongWritable lwTotalJobs = new LongWritable(totalJobs);
			for (long i = 0; i < totalJobs; i++)
			{
				lwJobId.set(i);
				output.collect(lwJobId, lwTotalJobs);
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
			totalJobs = (long) job.getInt("totalJobs", 1);
		}
	}

	/**
	 * The Class Reducer_GenerateEdges.
	 */
	public static class Reducer_GenerateEdges extends MapReduceBase implements
	    Reducer<LongWritable, LongWritable, LongWritable, LongWritable>
	{
		/*
		 * (non-Javadoc)
		 * 
		 * @see org.apache.hadoop.mapred.Reducer#reduce(java.lang.Object,
		 * java.util.Iterator, org.apache.hadoop.mapred.OutputCollector,
		 * org.apache.hadoop.mapred.Reporter)
		 */
		public void reduce(LongWritable LwJobId, Iterator<LongWritable> values,
		    OutputCollector<LongWritable, LongWritable> output, Reporter reporter)
		    throws IOException
		{
			/* the jobId is the key. */
			jobId = LwJobId.get();


			/*
			 * we could get the total jobs from the configuration parameters, but we
			 * do this to ensures things are working as expected.
			 */
			long totalRecordsRead = 0;
			while (values.hasNext())
			{
				/* keep track of the total records read, should be just one. */
				++totalRecordsRead;

				/* get the jobId and totalJobs. */
				LongWritable lwTotalJobs = values.next();
				totalJobs = lwTotalJobs.get();
			}

			if (totalRecordsRead != 1)
			{
				/* ouch, mapper should have only generated one record per key. */
				throw new RuntimeException(
				    "Mapper Mapper_MakeJobIdTotalJobsPairs create " + totalRecordsRead
				        + " records, but should have only made one record.");
			}

      /* make sure jobId is valid. */
      if ((jobId < 0) || (jobId >= totalJobs))
      {
        // ouch, jobId is out of range.
        throw new RuntimeException("Value of jobId, " + jobId
            + ", out of range, should be in [0," + totalJobs + "].");
      }
      
			/* create the graph edge generator. */
			GenerateGraphEdgesStreamingly graphEdgesGenerator = new GenerateGraphEdgesStreamingly(
			    totalComponents, totalNodes, totalEdges, totalJobs, jobId,
			    randomSeed, false, permuter, probabilities, skew, delimiter);

			/* iterate over the edges and push them to the output. */
			LongWritable fromNode = new LongWritable();
			LongWritable toNode = new LongWritable();
			while (graphEdgesGenerator.hasNext())
			{
				Edge nextEdge = graphEdgesGenerator.next();

				/* convert the edge to LongWritables. */
				fromNode.set(nextEdge.getFromVertex());
				toNode.set(nextEdge.getToVertex());

				output.collect(fromNode, toNode);
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
			totalEdges = job.getLong("totalEdges", 1);
			totalNodes = job.getLong("totalNodes", 2);
			totalComponents = job.getLong("totalComponents", 1);
			randomSeed = job.getLong("randomSeed", 0);
			permuter = job.getLong("permuter", 0);
			probabilities = new double[4];
			probabilities[0] = (double) job.getFloat("probabilities[0]", 0);
			probabilities[1] = (double) job.getFloat("probabilities[1]", 0);
			probabilities[2] = (double) job.getFloat("probabilities[2]", 0);
			probabilities[3] = (double) job.getFloat("probabilities[3]", 0);
			delimiter = job.get("delimiter", "\t");
			skew = (double) job.getFloat("skew", 1);
		}

		/** The total edges. */
		private long totalEdges;

		/** The total nodes. */
		private long totalNodes;

		/** The total components. */
		private long totalComponents;

		/** The random seed. */
		private long randomSeed;

		/** The total jobs. */
		private long totalJobs;

		/** The permuter. */
		private long permuter;

		/** The job id. */
		private long jobId;

		/** The probabilities. */
		private double[] probabilities;

		/** The delimiter. */
		private String delimiter;

		/** The skew. */
		private double skew;
	}

	/**
	 * The Class Reducer_GenerateVertexCompIds.
	 */
	public static class Reducer_GenerateVertexCompIds extends MapReduceBase
	    implements Reducer<LongWritable, LongWritable, Text, Text>
	{
		/*
		 * (non-Javadoc)
		 * 
		 * @see org.apache.hadoop.mapred.Reducer#reduce(java.lang.Object,
		 * java.util.Iterator, org.apache.hadoop.mapred.OutputCollector,
		 * org.apache.hadoop.mapred.Reporter)
		 */
		public void reduce(LongWritable LwJobId, Iterator<LongWritable> values,
		    OutputCollector<Text, Text> output, Reporter reporter)
		    throws IOException
		{
			/* the jobId is the key. */
			jobId = LwJobId.get();

			/*
			 * we could get the total jobs from the configuration parameters, but we
			 * do this to ensures things are working as expected.
			 */
			long totalRecordsRead = 0;
			while (values.hasNext())
			{
				/* keep track of the total records read, should be just one. */
				++totalRecordsRead;

				/* get the jobId and totalJobs. */
				LongWritable lwTotalJobs = values.next();
				totalJobs = lwTotalJobs.get();
			}

			if (totalRecordsRead != 1)
			{
				/* ouch, mapper should have only generated one record per key. */
				throw new RuntimeException(
				    "Mapper Mapper_MakeJobIdTotalJobsPairs create " + totalRecordsRead
				        + " records, but should have only made one record.");
			}

      /* make sure jobId is valid. */
      if ((jobId < 0) || (jobId >= totalJobs))
      {
        // ouch, jobId is out of range.
        throw new RuntimeException("Value of jobId, " + jobId
            + ", out of range, should be in [0," + totalJobs + "].");
      }
      
			/* create the vertex component-id generator. */
			GenerateGraphVertexCompIdsStreamingly graphGenerator = new GenerateGraphVertexCompIdsStreamingly(
			    totalComponents, totalNodes, totalJobs, jobId, randomSeed, permuter,
			    skew, delimiter);

			/* iterate over the vertex component-id pairs and push them to the output. */
			Text vertex = new Text();
			Text compId = new Text();
			while (graphGenerator.hasNext())
			{
				/* get the next vertex comp-id. */
				VertexCompId nextVertexCompId = graphGenerator.next();

				/* convert them to Text objects. */
				vertex.set(nextVertexCompId.getVertex());
				compId.set(nextVertexCompId.getCompId());

				/* dump them. */
				output.collect(vertex, compId);
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
			totalNodes = job.getLong("totalNodes", 2);
			totalComponents = job.getLong("totalComponents", 1);
			randomSeed = job.getLong("randomSeed", 0);
			permuter = job.getLong("permuter", 0);
			delimiter = job.get("delimiter", "\t");
			skew = (double) job.getFloat("skew", 1);
		}

		/** The total nodes the graph will have. */
		private long totalNodes;

		/** The total components the graph will have. */
		private long totalComponents;

		/** The random seed used to create the graph. */
		private long randomSeed;

		/** The total jobs to use to create the graph. */
		private long totalJobs;

		/** The value used to permute the vertices of the graph. */
		private long permuter;

		/** The job id for this job. */
		private long jobId;

		/** The delimiter to print between the vertex-id and component-id. */
		private String delimiter;

		/** The skew factor used to compute the size of the components of the graph. */
		private double skew;
	}
}

