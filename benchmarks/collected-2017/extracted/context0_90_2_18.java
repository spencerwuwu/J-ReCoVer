

	public void reduce(Text key, Iterable<IntWritable> values,
			Context context)
					throws IOException, InterruptedException {
		context.write(key, new IntWritable(1));
	}

