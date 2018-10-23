

	private final IntWritable count = new IntWritable(1);

	public void reduce(Text key, Iterator<IntWritable> values,
			OutputCollector<Text, IntWritable> output, Reporter reporter) throws IOException {
		output.collect(key, count);
		count.set(count.get() + 1);
	}  
