

	private LongWritable val = new LongWritable();

	public void reduce(Text key, Iterable<LongWritable> values, Context context)
			throws IOException, InterruptedException {

		int sum = 0;
		for (LongWritable value : values) {
			sum += value.get();
		}
		val.set(sum);
		context.write(key, val);
	}

