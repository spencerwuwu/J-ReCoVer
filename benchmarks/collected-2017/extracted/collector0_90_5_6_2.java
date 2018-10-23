

	public void reduce(final Text key, final Iterator<IntWritable> values, final OutputCollector<Text, IntWritable> output,
			final Reporter reporter) throws IOException {
		int count = 0;
		while (values.hasNext()) {
			count += values.next().get();
		}

		output.collect(key, new IntWritable(count));
	}
