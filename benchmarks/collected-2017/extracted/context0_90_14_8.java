

	private IntWritable frequency = new IntWritable();

	public void reduce(IntWritable key, Iterable<IntWritable> values, Context context)
			throws IOException, InterruptedException {

		int count = 0;
		for (IntWritable value : values) {
			count += value.get();
		}
		frequency.set(count);
		context.write(key, frequency);
		//       context.getCounter(Counters.TOTAL_PATENTS).increment(1L);
	}

