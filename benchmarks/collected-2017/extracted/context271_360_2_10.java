

	public final String USERS_COUNTER_NAME = "Users";
	private LongWritable outvalue = new LongWritable();

	public void reduce(Text key, Iterable<LongWritable> values,
			Context context) throws IOException, InterruptedException {

		// Increment user counter, as each reduce group represents one user
		//context.getCounter(AVERAGE_CALC_GROUP, USERS_COUNTER_NAME).increment(1);

		int sum = 0;

		for (LongWritable value : values) {
			sum += value.get();
		}

		outvalue.set(sum);
		context.write(key, outvalue);
	}

