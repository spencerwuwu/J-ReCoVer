

	public void reduce(IntWritable key, Iterable<IntWritable> it,
			Context context) throws IOException, InterruptedException {
		int keyint = key.get();
		int count = 0;
		for (IntWritable iw : it) {
			count++;
		}
		context.write(new IntWritable(keyint), new IntWritable(count));
	}

