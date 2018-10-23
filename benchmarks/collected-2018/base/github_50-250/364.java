// https://searchcode.com/api/result/68758594/

package org.manea.vlad.messageselector;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.Iterator;
import java.util.Random;
import java.util.Vector;

import org.apache.hadoop.mapred.*;
import org.apache.hadoop.io.*;

/**
 * is the message select reduce class
 * 
 * @author administrator
 */
@SuppressWarnings("deprecation")
class MessageSelectorReducer extends MapReduceBase implements
		Reducer<IntWritable, Text, IntWritable, Text> {

	// constant max
	static final IntWritable MAX = new IntWritable(Integer.MAX_VALUE);

	/**
	 * reduces (mix, word) to (mix, word)<br />
	 * only a number of selected messages
	 * 
	 * @param key
	 *            number of mix
	 * @param value
	 *            word for the mix
	 * @param output
	 *            output to write to
	 * @param reporter
	 *            reporter of actions
	 * @throws IOException
	 */
	@Override
	public void reduce(IntWritable key, Iterator<Text> values,
			OutputCollector<IntWritable, Text> output, Reporter reporter)
			throws IOException {

		// initialize an array of messages
		Vector<String> words = new Vector<String>();

		// initialize an array of booleans
		Vector<Boolean> selected = new Vector<Boolean>();

		// create a random number generator
		Random randomGenerator = new SecureRandom();

		// iterate all values in iterator
		while (values.hasNext()) {

			// add value to words and add true to selected
			words.add(values.next().toString());
			selected.add(true);

		}

		// set index and limit
		int index, limit = words.size()
				- MessageSelectorEntryPoint.getModerator()
						.getNumberOfMessages();

		// select up to limit messages to be discarded
		for (int step = 0; step < limit; ++step) {

			do {

				// select at random an index
				index = randomGenerator.nextInt(words.size());

			} while (!selected.elementAt(index));

			// discard message at index
			selected.set(index, false);

		}

		// iterate all messages
		for (index = 0; index < words.size(); ++index) {

			// output (mix, word) if word is not discarded and else (MAX, word)
			if (selected.elementAt(index)) {
				output.collect(key, new Text(words.elementAt(index)));
			} else {
				output.collect(MAX, new Text(words.elementAt(index)));
			}

		}

	}

}

