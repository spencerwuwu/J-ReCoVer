

	public void reduce(IntWritable key, Iterable<IntWritable> values,
			Context context) throws IOException, InterruptedException {
		int seen = 0;
		for (IntWritable value : values) {
			seen += value.get();
		}
		//assertTrue("Bad count for " + key.get(), verify(key.get(), seen));
		context.write(key, new IntWritable(seen));
	}


