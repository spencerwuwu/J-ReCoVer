

	Map<String, String> info = null;

	Text kword = new Text();
	LongWritable vword = new LongWritable();
	public void reduce(Text key, Iterable<LongWritable> values, Context context) throws IOException, InterruptedException
	{
		long sum = 0;
		for(LongWritable value : values)
		{
			sum = sum + value.get();
		}
		//String myKey = key.toString() + "\t" + info.get(key.toString());
		//kword.set(myKey);
		vword.set(sum);
		context.write(kword, vword);		
	}

