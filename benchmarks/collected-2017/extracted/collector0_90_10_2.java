

	public void reduce(Text key, Iterator<DoubleWritable> values,
			OutputCollector<Text, DoubleWritable> output, Reporter reporter)
					throws IOException {
		// Key is label,word, value is the number of times we've seen this label
		// word per local node. Output is the same

		double weightSumPerLabel = 0.0;

		while (values.hasNext()) {
			weightSumPerLabel += values.next().get();
		}
		output.collect(key, new DoubleWritable(weightSumPerLabel));

	}

