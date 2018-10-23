

	private IntWritable result = new IntWritable();

	public void reduce(Text key, Iterable<IntWritable> values, Context context)
			throws IOException, InterruptedException {
		//context.getCounter("MyCounterGroup", "REDUCE_INPUT_GROUPS").increment(1);
		int sum = 0;
		for (IntWritable val : values) {
			sum += val.get();
		}
		result.set(sum);
		context.write(key, result);
		//context.getCounter("MyCounterGroup", "REDUCE_OUTPUT_RECORDS").increment(1);
	}

