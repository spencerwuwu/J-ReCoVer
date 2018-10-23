// https://searchcode.com/api/result/68758341/

package org.manea.vlad.messagerandomizer;

import java.io.IOException;
import java.util.Iterator;

import org.apache.hadoop.mapred.*;
import org.apache.hadoop.io.*;
import org.manea.vlad.moderator.Recipient;

/**
 * is the message randomize redistribute reduce class
 * 
 * @author administrator
 */
class MessageRandomizerRedistributeReducer extends MessageRandomizerReducer {

	/**
	 * reduces (mix, word) to (mix, word), but in other order<br />
	 * if redistribute is true, messages are evenly redistributed to mixes<br />
	 * if redistribute is false, messages are all redistributed to the next mix
	 * 
	 * @param key
	 *            number of mix
	 * @param value
	 *            word for the mix
	 * @param output
	 *            -i output to write to
	 * @param reporter
	 *            reporter of actions
	 * @throws IOException
	 */
	@Override
	public void reduce(IntWritable key, Iterator<Text> values,
			OutputCollector<IntWritable, Text> output, Reporter reporter)
			throws IOException {

		// call parent
		super.reduce(key, values, output, reporter);

		// get the number of messages per mix
		int messagesPerMix = words.size()
				/ MessageRandomizerEntryPoint.getModerator().getNumberOfMixes();

		// iterate through all messages
		for (int index = 0; index < words.size(); ++index) {

			// get the mix to be given this message
			int mix = permutation.elementAt(index) / messagesPerMix;

			try {

				// output (mix, word)
				output.collect(new IntWritable(mix), new Text(Recipient
						.getElGamal().reencryptLongString(words.elementAt(index))));

			} catch (Exception exception) {

				// throw further exception
				throw new IOException();

			}

		}

	}

}

