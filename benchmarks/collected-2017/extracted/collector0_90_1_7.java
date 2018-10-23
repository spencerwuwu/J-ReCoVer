

	public void reduce(IntWritable key, Iterator<IntWritable> values, OutputCollector<IntWritable, IntWritable> output, Reporter reporter) throws IOException {
		int count = 0;
		while (values.hasNext()) count += values.next().get();
		output.collect(key, new IntWritable(count));
	}

