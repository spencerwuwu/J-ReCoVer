

	private final IntWritable cnt = new IntWritable(1);

	public void reduce(Text key, Iterator<IntWritable> values,
			OutputCollector<Text, IntWritable> output, Reporter reporter) throws IOException {
		output.collect(key, cnt);
		cnt.set(cnt.get() + 1);
	}

