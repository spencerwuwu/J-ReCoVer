

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

