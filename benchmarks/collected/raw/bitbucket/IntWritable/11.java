// https://searchcode.com/api/result/48555502/

/***********************************************************************************************************************
 *
 * Copyright (C) 2010 by the Stratosphere project (http://stratosphere.eu)
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 **********************************************************************************************************************/
package eu.stratosphere.journalpaper.trienum.hadoop;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.mapred.FileInputFormat;
import org.apache.hadoop.mapred.FileOutputFormat;
import org.apache.hadoop.mapred.JobClient;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.MapReduceBase;
import org.apache.hadoop.mapred.Mapper;
import org.apache.hadoop.mapred.OutputCollector;
import org.apache.hadoop.mapred.Reducer;
import org.apache.hadoop.mapred.Reporter;
import org.apache.hadoop.mapred.SequenceFileOutputFormat;
import org.apache.hadoop.util.Tool;

import eu.stratosphere.journalpaper.trienum.hadoop.io.EdgeInputFormat;
import eu.stratosphere.journalpaper.trienum.hadoop.io.type.IntPair;
import eu.stratosphere.journalpaper.trienum.hadoop.io.type.IntTriple;

@SuppressWarnings("deprecation")
public class BuildTriads extends Configured implements Tool {

	/**
	 * Assign the lower edge vertex as key, use the other as value.
	 */
	public static class AssignKey extends MapReduceBase implements
			Mapper<NullWritable, IntPair, IntWritable, IntWritable> {

		private IntWritable outKey = new IntWritable();

		private IntWritable outVal = new IntWritable();

		@Override
		public void map(NullWritable key, IntPair edge, OutputCollector<IntWritable, IntWritable> output,
				Reporter reporter) throws IOException {
			if (edge.getFirst() < edge.getSecond()) {
				this.outKey.set(edge.getFirst());
				this.outVal.set(edge.getSecond());
			} else {
				this.outKey.set(edge.getSecond());
				this.outVal.set(edge.getFirst());
			}
			output.collect(this.outKey, this.outVal);
		}
	}

	/**
	 * Build triads by crossing all vertices incident to the same neighbor.
	 */
	public static class BuildTriad extends MapReduceBase implements
			Reducer<IntWritable, IntWritable, NullWritable, IntTriple> {

		private final NullWritable outKey = NullWritable.get();

		private IntTriple outVal = new IntTriple();

		private final List<Integer> seenNodes = new ArrayList<Integer>();

		@Override
		public void reduce(IntWritable key, Iterator<IntWritable> values,
				OutputCollector<NullWritable, IntTriple> output, Reporter reporter) throws IOException {

			// clear seen nodes
			this.seenNodes.clear();

			while (values.hasNext()) {
				// get current node from value
				int v1 = values.next().get();
				// pair the current node with all 'seen' nodes
				for (int v2 : this.seenNodes) {
					if (v1 < v2) {
						this.outVal.set(v1, key.get(), v2);
					} else {
						this.outVal.set(v2, key.get(), v1);
					}
					output.collect(this.outKey, this.outVal);
				}
				// add to seen nodes
				this.seenNodes.add(v1);
			}
		}
	}

	@Override
	public int run(String[] args) throws Exception {

		// check number of command line parameters
		if (args.length < 3) {
			throw new IllegalArgumentException("Usage: [dop] [edges_path] [triads_path]");
		}
		
		// parse command line parameters		
		int dop = Integer.parseInt(args[0]);
		Path edgesPath = new Path(args[1]);
		Path triadsPath = new Path(args[2]);

		JobConf conf = new JobConf(getConf(), BuildTriads.class);
		conf.setJobName("Build Triads");

		conf.setMapOutputKeyClass(IntWritable.class);
		conf.setMapOutputValueClass(IntWritable.class);
		conf.setOutputKeyClass(NullWritable.class);
		conf.setOutputValueClass(IntTriple.class);
		
		conf.setMapperClass(AssignKey.class);
		conf.setReducerClass(BuildTriad.class);
		conf.setNumReduceTasks(dop);

		conf.setInputFormat(EdgeInputFormat.class);
//		conf.setOutputFormat(TextOutputFormat.class);
		conf.setOutputFormat(SequenceFileOutputFormat.class);

		FileInputFormat.setInputPaths(conf, edgesPath);
		FileOutputFormat.setOutputPath(conf, triadsPath);

		// as the output key is of type NullWritable, we want to
		// use empty separator for the output (key, value) pair
		conf.set("mapred.textoutputformat.separator", "");

		JobClient.runJob(conf);
		return 0;
	}
}

