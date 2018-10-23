

	public final double D = 0.15;
	public void reduce(IntWritable key, Iterable<DoubleWritable> values, Context context) throws IOException, InterruptedException 
	{
		ArrayList<Double> diffs = new ArrayList<Double>();//list of differences computed
		for (DoubleWritable diffValue : values)
		{
			diffs.add(diffValue.get());
		}

		Collections.sort(diffs);
		double maxDiff = diffs.get(diffs.size()-1);//get last value (largest diff)

		context.write(null, new DoubleWritable(maxDiff));
	}

