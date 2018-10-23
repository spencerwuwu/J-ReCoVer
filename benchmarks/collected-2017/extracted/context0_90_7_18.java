

	private IntWritable result = new IntWritable();

	public void reduce(Text key, Iterable<IntWritable> values, 
			Context context
			) throws IOException, InterruptedException {
		System.out.println("Reducers Sort and Shuffle Merge Brings keys Over HTTP"+ key +" and there  Values  our case its <key> <v1> <v1> each being 1 " );

		int sum = 0;
		for (IntWritable val : values) {
			sum += val.get();
		}
		result.set(sum);
		context.write(key, result);
	}

