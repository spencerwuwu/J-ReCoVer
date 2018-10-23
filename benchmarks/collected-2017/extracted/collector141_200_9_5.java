

	private final IntWritable cnt = new IntWritable(1);

	public void reduce(IntWritable key, Iterator<IntWritable> values,
			OutputCollector<IntWritable, IntWritable> output, Reporter reporter) throws IOException {
		output.collect(key, cnt);
		cnt.set(cnt.get() + 1);
	}

