

	private IntWritable totalWordCount = new IntWritable();

	public void reduce(Text key, Iterator<IntWritable> values, OutputCollector<Text, IntWritable> output,
			Reporter reporter) throws IOException {
		int wordCount = 0;
		while (values.hasNext()) {
			wordCount += ((IntWritable) values.next()).get();
		}

		this.totalWordCount.set(wordCount);
		output.collect(key, this.totalWordCount);
	}

