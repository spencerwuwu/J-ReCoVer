

	public void reduce(Text text, Iterable<IntWritable> iterable, Context context)
			throws IOException, InterruptedException {

		int result = 0;

		for (IntWritable iterator : iterable)
			result += iterator.get();

		context.write(text, new IntWritable(result));
	}

