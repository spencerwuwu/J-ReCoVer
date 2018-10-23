

	private final IntWritable sCnt = new IntWritable(1);

	public void reduce(Text key, Iterator<IntWritable> values,
			OutputCollector<Text, IntWritable> output, Reporter reporter) throws IOException {
		output.collect(key, sCnt);
		sCnt.set(sCnt.get() + 1);
	}

