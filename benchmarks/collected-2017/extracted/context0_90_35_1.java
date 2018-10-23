

	public void reduce(Text key, Iterable<LongWritable> values, Context context) throws IOException,
	InterruptedException {
		long sum = 0;
		for (LongWritable value : values) {
			//context.setStatus("Parallel Counting Reducer :" + key);
			sum += value.get();
		}
		//context.setStatus("Parallel Counting Reducer: " + key + " => " + sum);
		context.write(key, new LongWritable(sum));
	}

