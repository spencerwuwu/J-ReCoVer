

	public void reduce(Text key, Iterator<LongWritable> values,
			OutputCollector<Text, LongWritable> collect, Reporter reporter)
					throws IOException {
		long sum = 0;
		while(values.hasNext())
		{
			sum = sum + values.next().get();
		}
		collect.collect(key, new LongWritable(sum));
	}
