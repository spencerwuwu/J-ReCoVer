

	/** Writes all keys and values directly to output. */
	public void reduce(LongWritable key, Iterator<LongWritable> values,
			OutputCollector<LongWritable, LongWritable> output, Reporter reporter)
					throws IOException {
		while (values.hasNext()) {
			output.collect(key, values.next());
		}
	}

