

	public void reduce(IntWritable key, Iterable<DoubleWritable> values,
			Context context) throws IOException, InterruptedException {
		double sum = 0.0;
		for (DoubleWritable value : values) {
			sum += value.get();
		}
		context.write(key, new DoubleWritable(sum));
	}

