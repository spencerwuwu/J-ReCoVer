

	private IntWritable result = new IntWritable();

	public void reduce(Text key, Iterable<IntWritable> values, 
            Context context) throws IOException, InterruptedException {
		int sum = 0; // initialize the sum for each keyword
		for (IntWritable val : values) {
			sum += val.get();  
		}
		result.set(sum);

		context.write(key, result); // create a pair <keyword, number of occurences>
	}

