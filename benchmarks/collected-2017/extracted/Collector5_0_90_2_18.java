

	public void reduce(LongWritable key, Iterator<LongWritable> values,
			OutputCollector<LongWritable, LongWritable> output,
			Reporter reporter)
					throws IOException {

		// sum all values for this key
		long sum = 0;
		while (values.hasNext()) {
			sum += values.next().get();
		}

		// output sum
		output.collect(key, new LongWritable(sum));
	}

