// https://searchcode.com/api/result/68758301/

package org.manea.vlad.messagerandomizer;

import java.io.IOException;
import java.util.Iterator;

import org.apache.hadoop.mapred.*;
import org.apache.hadoop.io.*;
import org.manea.vlad.moderator.Recipient;

/**
 * is the message randomize proceed reduce class
 * 
 * @author administrator
 */
class MessageRandomizerProceedReducer extends MessageRandomizerReducer {

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

		// get next mix
		IntWritable nextKey = new IntWritable((key.get() + 1)
				% MessageRandomizerEntryPoint.getModerator().getNumberOfMixes());

		try {

			// iterate messages and output them permuted and re-encrypted
			for (int index = 0; index < words.size(); ++index) {

				output.collect(nextKey, new Text(Recipient.getElGamal()
						.reencryptLongString(words.get(permutation.get(index)))));

			}

		} catch (Exception exception) {

			// throw further exception
			exception.printStackTrace();
			throw new IOException();

		}

	}
}

