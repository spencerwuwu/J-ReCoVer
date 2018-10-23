// https://searchcode.com/api/result/68758556/

package org.manea.vlad.assignedmixcounter;

import java.io.IOException;
import java.util.Iterator;

import org.apache.hadoop.mapred.*;
import org.apache.hadoop.io.*;

/**
 * is the assigned mix count reduce class
 * 
 * @author administrator
 */
@SuppressWarnings("deprecation")
class AssignedMixCounterReducer extends MapReduceBase implements
		Reducer<IntWritable, IntWritable, IntWritable, IntWritable> {

	/**
	 * reduces (mix, 1) to (mix, count(mix))
	 * 
	 * @param key
	 *            number of mix
	 * @param value
	 *            counter of mixes
	 * @param output
	 *            output to write to
	 * @param reporter
	 *            reporter of actions
	 * @throws IOException
	 */
	@Override
	public void reduce(IntWritable key, Iterator<IntWritable> values,
			OutputCollector<IntWritable, IntWritable> output, Reporter reporter)
			throws IOException {

		// initialize sum value
		int counter = 0;

		// iterate all values in iterator
		while (values.hasNext()) {

			// add count to sum
			counter += values.next().get();

		}

		// output (mix, count(mix))
		output.collect(key, new IntWritable(counter));

	}

}

