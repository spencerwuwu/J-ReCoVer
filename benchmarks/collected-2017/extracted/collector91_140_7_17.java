

	private final IntWritable SumValue = new IntWritable();

	public void reduce(Text key, Iterator<IntWritable> values,
			OutputCollector<Text, IntWritable> output, Reporter reporter) throws IOException {
		// sum up values
		int sum = 0;
		while (values.hasNext()) {
			sum += values.next().get();
		}
		SumValue.set(sum);
		output.collect(key, SumValue);
	}	  
