

	public void reduce(Text key, Iterable<LongWritable> values,
			Context context)
					throws IOException, InterruptedException {
		LongWritable maxValue = null;
		for (LongWritable value : values) {
			if (maxValue == null) {
				maxValue = value;
			} else if (value.compareTo(maxValue) > 0) {
				maxValue = value;
			}
		}
		context.write(key, maxValue);
	}

