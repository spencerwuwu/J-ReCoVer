

	private final IntWritable cnt = new IntWritable(1);

	public void reduce(IntWritable key, Iterable<IntWritable> values, Context context)
			throws IOException, InterruptedException {
		context.write(key, cnt);
		cnt.set(cnt.get() + 1);
	}


