

	public void reduce(Text key, Iterator<LongWritable> values,
			OutputCollector<Text, LongWritable> output, Reporter reporter)
					throws IOException {
		LongWritable maxValue = null;
		while (values.hasNext()) {
			LongWritable value = values.next();
			if (maxValue == null) {
				maxValue = value;
			} else if (value.compareTo(maxValue) > 0) {
				maxValue = value;
			}
		}
		output.collect(key, maxValue);
	}

