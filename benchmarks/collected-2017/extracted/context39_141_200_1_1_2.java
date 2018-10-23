

	public void reduce(Text key, Iterable<IntWritable> values, Context context
			) throws IOException, InterruptedException {
		for(IntWritable value: values) {
			context.write((Text) key, (IntWritable) value);
		}
	}

