

	public void reduce(Text key, Iterable<IntWritable> values, Context context) throws IOException,
	InterruptedException {
		int sum = 0;
		for (IntWritable value : values) {
			sum += value.get();
		}
		context.write(key, new Text(sum + ""));
		//context.getCounter(EMRDriver.STATE_COUNTER_GROUP, EMRDriver.TOTAL_PROFILE_COUNT).increment(1);

	}

