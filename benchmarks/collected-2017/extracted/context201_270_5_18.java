

	private IntWritable val = new IntWritable();

	public void reduce(IntWritable key, Iterable<IntWritable> values,
			Context context) throws IOException, InterruptedException {

		int sum = 0;
		for (IntWritable value : values) {
			sum += value.get();
		}
		val.set(sum);
		context.write(key, val);
	}

