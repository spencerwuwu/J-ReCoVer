

	public void reduce(Text artist, Iterable<IntWritable> values,
			Context context) throws IOException,
	InterruptedException {
		context.write(artist, new IntWritable(0));
	}

