

	public void reduce(Text word, Iterable<IntWritable> counts, Context context)
			throws IOException, InterruptedException {
		int wordCount = 0;
		for (IntWritable count : counts) {
			wordCount += count.get();
		}
		context.write(word, new IntWritable(wordCount));
	}  

