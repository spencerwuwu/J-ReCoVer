// https://searchcode.com/api/result/48555501/

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
import java.util.Iterator;

import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.WritableComparable;
import org.apache.hadoop.io.WritableComparator;
import org.apache.hadoop.mapred.FileOutputFormat;
import org.apache.hadoop.mapred.JobClient;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.MapReduceBase;
import org.apache.hadoop.mapred.Mapper;
import org.apache.hadoop.mapred.OutputCollector;
import org.apache.hadoop.mapred.Partitioner;
import org.apache.hadoop.mapred.Reducer;
import org.apache.hadoop.mapred.Reporter;
import org.apache.hadoop.mapred.SequenceFileInputFormat;
import org.apache.hadoop.mapred.lib.MultipleInputs;
import org.apache.hadoop.util.Tool;

import eu.stratosphere.journalpaper.trienum.hadoop.io.EdgeInputFormat;
import eu.stratosphere.journalpaper.trienum.hadoop.io.TriadInputFormat;
import eu.stratosphere.journalpaper.trienum.hadoop.io.type.IntPair;
import eu.stratosphere.journalpaper.trienum.hadoop.io.type.IntTriple;
import eu.stratosphere.journalpaper.trienum.hadoop.io.type.TaggedIntPair;

@SuppressWarnings("deprecation")
public class CloseTriads extends Configured implements Tool {

	public static class EdgeTagger extends MapReduceBase implements
			Mapper<NullWritable, IntPair, TaggedIntPair, IntWritable> {

		private final TaggedIntPair outKey = new TaggedIntPair(0);

		private final IntWritable outVal = new IntWritable(-1);

		@Override
		public void map(NullWritable key, IntPair val, OutputCollector<TaggedIntPair, IntWritable> collector,
				Reporter reporter) throws IOException {

			if (val.getFirst() < val.getSecond()) {
				this.outKey.setVal(val.getFirst(), val.getSecond());
				collector.collect(this.outKey, this.outVal);
			} else {
				this.outKey.setVal(val.getSecond(), val.getFirst());
				collector.collect(this.outKey, this.outVal);
			}
		}
	}

	public static class TriadTagger extends MapReduceBase implements
			Mapper<NullWritable, IntTriple, TaggedIntPair, IntWritable> {

		private TaggedIntPair outKey = new TaggedIntPair(1);

		private IntWritable outVal = new IntWritable();

		@Override
		public void map(NullWritable key, IntTriple val, OutputCollector<TaggedIntPair, IntWritable> collector,
				Reporter reporter) throws IOException {

			this.outKey.setVal(val.getFirst(), val.getThird());
			this.outVal.set(val.getSecond());
			collector.collect(this.outKey, this.outVal);
		}
	}

	public static class CloseTriad extends MapReduceBase implements
			Reducer<TaggedIntPair, IntWritable, NullWritable, IntTriple> {

		private final NullWritable outKey = NullWritable.get();

		private IntTriple outVal = new IntTriple();

		@Override
		public void reduce(TaggedIntPair key, Iterator<IntWritable> values,
				OutputCollector<NullWritable, IntTriple> output, Reporter reporter) throws IOException {

			// check existance of closing edge in input
			int val = values.next().get();
			if (val >= 0) {
				return;
			}

			this.outVal.setSecond(key.getVal().getFirst());
			this.outVal.setThird(key.getVal().getSecond());

			while (values.hasNext()) {
				val = values.next().get();
				if (val >= 0) {
					this.outVal.setFirst(val);
					output.collect(this.outKey, this.outVal);
					break;
				}
			}

			// emit all triads
			while (values.hasNext()) {
				val = values.next().get();
				this.outVal.setFirst(val);
				output.collect(this.outKey, this.outVal);
			}
		}

	}

	public static class TaggedIntWritablePartitioner implements Partitioner<TaggedIntPair, IntWritable> {

		@Override
		public void configure(JobConf arg0) {
		}

		@Override
		public int getPartition(TaggedIntPair key, IntWritable val, int numPartitions) {
			return Math.abs(key.getVal().hashCode() * 163) % numPartitions;
		}
	}

	public static class TaggedIntPairFullComparator extends WritableComparator {

		protected TaggedIntPairFullComparator() {
			super(TaggedIntPair.class, true);
		}

		@Override
		@SuppressWarnings("rawtypes")
		public int compare(WritableComparable wc1, WritableComparable wc2) {
			TaggedIntPair tp1 = ((TaggedIntPair) wc1);
			TaggedIntPair tp2 = ((TaggedIntPair) wc2);
			return tp1.compareTo(tp2);
		}
	}

	public static class TaggedIntPairValueOnlyComparator extends WritableComparator {

		protected TaggedIntPairValueOnlyComparator() {
			super(TaggedIntPair.class, true);
		}

		@Override
		@SuppressWarnings("rawtypes")
		public int compare(WritableComparable wc1, WritableComparable wc2) {
			IntPair tp1 = ((TaggedIntPair) wc1).getVal();
			IntPair tp2 = ((TaggedIntPair) wc2).getVal();
			return tp1.compareTo(tp2);
		}
	}

	@Override
	public int run(String[] args) throws Exception {

		// check number of command line parameters
		if (args.length < 4) {
			throw new IllegalArgumentException("Usage: [dop] [input_edges] [input_triads] [output]");
		}

		// parse command line parameters
		int dop = Integer.parseInt(args[0]);
		Path edgesPath = new Path(args[1]);
		Path triadsPath = new Path(args[2]);
		Path outputPath = new Path(args[3]);

		// configure job
		JobConf conf = new JobConf(getConf(), CloseTriad.class);
		conf.setJobName("Close Triads");

		conf.setMapOutputKeyClass(TaggedIntPair.class);
		conf.setMapOutputValueClass(IntWritable.class);
		conf.setOutputKeyClass(NullWritable.class);
		conf.setOutputValueClass(IntTriple.class);

		conf.setPartitionerClass(TaggedIntWritablePartitioner.class);
		conf.setOutputKeyComparatorClass(TaggedIntPairFullComparator.class);
		conf.setOutputValueGroupingComparator(TaggedIntPairValueOnlyComparator.class);

		conf.setReducerClass(CloseTriad.class);
		conf.setNumReduceTasks(dop);

		MultipleInputs.addInputPath(conf, edgesPath, EdgeInputFormat.class, EdgeTagger.class);
		MultipleInputs.addInputPath(conf, triadsPath, SequenceFileInputFormat.class, TriadTagger.class);
		FileOutputFormat.setOutputPath(conf, outputPath);

		// as the output key is of type NullWritable, we want to
		// use empty separator for the output (key, value) pair
		conf.set("mapred.textoutputformat.separator", "");
		
		JobClient.runJob(conf);
		return 0;
	}
}

