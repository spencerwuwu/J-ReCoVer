

	private LongWritable sum = new LongWritable();

	public void reduce(Text key, Iterable<LongWritable> values, Context context)
			throws IOException, InterruptedException {

		int theSum = 0;
		for (LongWritable val : values) {
			theSum += val.get();
		}
		sum.set(theSum);
		context.write(key, sum);
	}

