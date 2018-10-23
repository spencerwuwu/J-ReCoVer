

	public void reduce(IntWritable key, Iterator<IntWritable> it,
			OutputCollector<IntWritable, IntWritable> out,
			Reporter reporter) throws IOException {
		int keyint = key.get();
		int total = 0;
		while (it.hasNext()) {
			total += it.next().get();
		}
		out.collect(new IntWritable(keyint), new IntWritable(total));
	}

