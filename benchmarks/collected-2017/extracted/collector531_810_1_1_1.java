

	public void reduce(IntWritable key, Iterator<IntWritable> it,
			OutputCollector<IntWritable, IntWritable> out,
			Reporter reporter) throws IOException {
		while (it.hasNext()) {
			out.collect(it.next(), null);
		}
	}

