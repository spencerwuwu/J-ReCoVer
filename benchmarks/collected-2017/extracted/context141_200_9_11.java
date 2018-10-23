

	// Reuse objects
	private final IntWritable sumWritable = new IntWritable();

	public void reduce(Text key, Iterable<IntWritable> values, Context context)	throws IOException, InterruptedException {
		// sum up values
		int sum = 0;
		Iterator<IntWritable> iter = values.iterator();
		while (iter.hasNext()) {
			sum += iter.next().get();
		}
		sumWritable.set(sum);
		context.write(key, sumWritable);
	}

