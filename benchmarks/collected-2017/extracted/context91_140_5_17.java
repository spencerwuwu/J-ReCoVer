

	Text vword = new Text();
	public void reduce(Text key, Iterable<LongWritable> values, Context context) throws IOException, InterruptedException
	{
		long sum = 0;
		double avg = 0.0;
		int counter = 0;
		for(LongWritable value : values)
		{
			sum = sum + value.get();
			counter++;
		}
		avg = (double) sum / counter;
		vword.set("sum: " + sum + "\tAverage: " + avg);
		context.write(key, vword);
	}

