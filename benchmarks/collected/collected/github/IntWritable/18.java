// https://searchcode.com/api/result/68758421/

package org.manea.vlad.generalminimumcomputer;

import java.io.IOException;
import java.util.Iterator;

import org.apache.hadoop.mapred.*;
import org.apache.hadoop.io.*;

/**
 * is the general minimum compute reduce class
 * 
 * @author administrator
 */
@SuppressWarnings("deprecation")
class GeneralMinimumComputerReducer extends MapReduceBase implements
		Reducer<BooleanWritable, IntWritable, BooleanWritable, IntWritable> {

	// constant true
	static final BooleanWritable TRUE = new BooleanWritable(true);

	/**
	 * reduces (TRUE, count) to (TRUE, minimum)
	 * 
	 * @param key
	 *            TRUE
	 * @param value
	 *            count of messages assigned to mix
	 * @param output
	 *            output to write to
	 * @param reporter
	 *            reporter of actions
	 * @throws IOException
	 */
	@Override
	public void reduce(BooleanWritable key, Iterator<IntWritable> values,
			OutputCollector<BooleanWritable, IntWritable> output,
			Reporter reporter) throws IOException {

		// initialize current count and minimum value
		int count, min = Integer.MAX_VALUE;

		// iterate all values in iterator
		while (values.hasNext()) {

			// get count and update minimum
			count = values.next().get();
			min = (min > count ? count : min);

		}

		// update minimum as multiple of n
		min = Math.min(min, min
				/ GeneralMinimumComputerEntryPoint.getModerator()
						.getNumberOfMixes()
				* GeneralMinimumComputerEntryPoint.getModerator()
						.getNumberOfMixes());

		// output (TRUE, count(mix))
		output.collect(TRUE, new IntWritable(min));

	}

}

