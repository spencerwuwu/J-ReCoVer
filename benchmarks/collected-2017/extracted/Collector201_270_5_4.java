

	private LongWritable outvalue = new LongWritable();

	public void reduce(Text key, Iterator<LongWritable> values,
			OutputCollector<Text, LongWritable> output, Reporter reporter)
					throws IOException {

		int sum = 0;
		while (values.hasNext()) {
			sum += values.next().get();
		}
		outvalue.set(sum);
		output.collect(key, outvalue);
	}

