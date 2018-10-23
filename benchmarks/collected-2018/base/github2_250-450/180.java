// https://searchcode.com/api/result/68758317/

package org.manea.vlad.messagerandomizer;

import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.SecureRandom;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Random;
import java.util.Vector;
import java.util.Queue;

import org.apache.hadoop.mapred.*;
import org.apache.hadoop.io.*;
import org.manea.vlad.moderator.Moderator;

/**
 * is the message randomize reduce class
 * 
 * @author administrator
 */
@SuppressWarnings("deprecation")
abstract class MessageRandomizerReducer extends MapReduceBase implements
		Reducer<IntWritable, Text, IntWritable, Text> {

	// constant dummy message
	/* private final String dummyMessageContents = "dummy"; */

	// initialize an array of messages
	protected Vector<String> words = new Vector<String>();

	// initialize an array of booleans
	protected Vector<Integer> permutation;

	// queues
	private Vector<Queue<String>> queues = new Vector<Queue<String>>();

	// set number of read messages
	/* private int readMessages = 0; */

	// set current queue
	private int currentQueue = 0;

	// create buffered reader
	private BufferedReader bufferedReader = null;

	// create print writer
	private PrintWriter printWriter = null;

	// create a random number generator
	Random randomGenerator = new SecureRandom();

	/**
	 * returns a random permutation of given size
	 * 
	 * @param size
	 *            the size of permutation
	 * @return a vector with given permutation
	 */
	protected Vector<Integer> getRandomPermutation(int size) {

		// create an array of size
		Vector<Integer> permutation = new Vector<Integer>();

		// iterate through the array and set identity permutation
		for (int index = 0; index < size; ++index)
			permutation.add(index);

		// iterate through the array and set random swap
		for (int index = 0; index < size; ++index)
			Collections.swap(permutation, index,
					index + randomGenerator.nextInt(size - index));

		// return the constructed array
		return permutation;

	}

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

		// remove words
		words.clear();

		// remove queues
		queues.clear();

		// iterate queues
		for (int index = 0; index < Moderator.INSTANCE.getNumberOfQueues(); ++index)
			queues.add(new LinkedList<String>());

		try {

			// read from mix file
			// readFromMixFile(key.get());

		} finally {

			// close buffer reader
			if (bufferedReader != null)
				bufferedReader.close();

		}

		// add dummy messages
		/*
		 * addDummyMessages(Moderator.INSTANCE.getNumberOfMemory() -
		 * readMessages, key.get());
		 */

		// add input messages
		int indexInput = addInputMessages(values);

		// check for multiple of n messages
		if (indexInput
				% MessageRandomizerEntryPoint.getModerator().getNumberOfMixes() != 0)
			throw new IOException();

		// add to words from queues
		addFromQueuesToWords(indexInput);

		// write messages to file
		writeToMixFile(key.get());

		// set random permutation
		permutation = getRandomPermutation(indexInput);

	}

	/**
	 * writes back to the file the remaining messages
	 * 
	 * @param key
	 *            the key
	 * @throws IOException
	 */
	private void writeToMixFile(int key) throws IOException {

		// create print writer
		printWriter = new PrintWriter(new FileWriter(
				Moderator.INSTANCE.getMixFile(key), false));

		// print the first number
		printWriter.println(currentQueue);

		// print all messages in queues
		for (int index = 0; index < Moderator.INSTANCE.getNumberOfQueues(); ++index) {

			// iterate queue and write message
			while (!queues.get(index).isEmpty())
				printWriter.println(index + " " + queues.get(index).poll());

		}

	}

	/**
	 * adds messages to words from queues
	 * 
	 * @param indexInput
	 *            the number of words extracted from the queues
	 */
	private void addFromQueuesToWords(int indexInput) {

		// add until index input
		for (int index = 0; index < indexInput;) {

			// iterate the current queue
			for (; !queues.get(currentQueue).isEmpty() && index < indexInput; ++index) {

				// take a message from the queue
				words.add(queues.get(currentQueue).poll());

			}

			// go to the next queue
			currentQueue = (currentQueue + 1)
					% Moderator.INSTANCE.getNumberOfQueues();

		}

	}

	/**
	 * adds input messages
	 * 
	 * @param values
	 *            the iterator with messages
	 * @return the number of read messages
	 * @throws IOException
	 */
	private int addInputMessages(Iterator<Text> values) throws IOException {

		// number of input messages
		int indexInput = 0;

		// iterate all values in iterator and add them to words
		while (values.hasNext()) {

			// add input message to queue
			queues.get(
					randomGenerator.nextInt(Moderator.INSTANCE
							.getNumberOfQueues()))
					.add(values.next().toString());

			// new input message
			++indexInput;

		}

		// return index input
		return indexInput;

	}

	/**
	 * adds dummy messages
	 * 
	 * @param count
	 *            the number of dummy messages to be added
	 * @param key
	 *            the key
	 */
	/*
	 * private void addDummyMessages(int count, int key) {
	 * 
	 * // add dummy messages for (int jndex = 0; jndex < count; ++jndex) {
	 * 
	 * // set initial content String initial = dummyMessageContents + " " + key
	 * + " " + randomGenerator.nextLong();
	 * 
	 * System.out.println("to encrypt: " + initial);
	 * 
	 * // encrypt initial content String content =
	 * Recipient.getElGamal().encryptLongString(initial);
	 * 
	 * System.out.println("to have: " + content);
	 * 
	 * // add dummy message to queue
	 * 
	 * queues.get( randomGenerator.nextInt(Moderator.INSTANCE
	 * .getNumberOfQueues())).add(content);
	 * 
	 * }
	 * 
	 * }
	 */

	/**
	 * reads from mix file
	 * 
	 * @param key
	 *            the key
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	/*
	 * private void readFromMixFile(int key) throws FileNotFoundException,
	 * IOException {
	 * 
	 * // create buffered reader bufferedReader = new BufferedReader(new
	 * FileReader( Moderator.INSTANCE.getMixFile(key)));
	 * 
	 * // create line string String line;
	 * 
	 * // read first line if ((line = bufferedReader.readLine()) == null)
	 * return;
	 * 
	 * // read current queue currentQueue = Integer.parseInt(line);
	 * 
	 * // set message queue int messageQueue;
	 * 
	 * // read lines while ((line = bufferedReader.readLine()) != null) {
	 * 
	 * // add space token extractor StringTokenizer spaceTokenizer = new
	 * StringTokenizer(line);
	 * 
	 * // check for initial number existence if
	 * (!spaceTokenizer.hasMoreTokens()) throw new IOException();
	 * 
	 * // lose the first number messageQueue =
	 * Integer.parseInt(spaceTokenizer.nextToken());
	 * 
	 * // check for second number existence if (!spaceTokenizer.hasMoreTokens())
	 * throw new IOException();
	 * 
	 * // add message to queue
	 * queues.get(messageQueue).add(spaceTokenizer.nextToken());
	 * 
	 * // a new read message ++readMessages;
	 * 
	 * // break at too many messages if (readMessages >=
	 * Moderator.INSTANCE.getNumberOfMemory()) return;
	 * 
	 * }
	 * 
	 * }
	 */

}

