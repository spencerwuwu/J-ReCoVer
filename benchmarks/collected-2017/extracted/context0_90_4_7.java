

	FloatWritable vword = new FloatWritable();
	public void reduce(Text key, Iterable<LongWritable> values, Context context) throws IOException, InterruptedException
	{
		float ratingAvg = 0.0f;
		long sum = 0;
		int counter = 0;
		for(LongWritable value : values)
		{
			counter++;
			sum = sum + value.get();
		}
		ratingAvg = (float) (sum / counter);
		vword.set(ratingAvg);
		context.write(key, vword);
	}

