

	public void reduce(Text key, Iterable<IntWritable> values, Context context) throws IOException, InterruptedException {
		int maxValue = Integer.MIN_VALUE;
		for(IntWritable value : values){
			maxValue = Math.max(maxValue, value.get());
		}
		context.write(key,new IntWritable(maxValue));
	}

