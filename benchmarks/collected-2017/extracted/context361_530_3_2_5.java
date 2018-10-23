

	public void reduce(Text key, Iterable<IntWritable> values, Context context)
			throws IOException, InterruptedException {
		IntWritable result = new IntWritable();
		int sum = 0;
		for (IntWritable val : values) {
			sum += val.get();
		}
		result.set(sum);
		context.write(key, result);
		//context.getCounter(ProjectCounters.COMPONENTS).increment(1);
		//context.getCounter(ProjectCounters.SQUAREDSUM).increment(sum * sum);
	}

