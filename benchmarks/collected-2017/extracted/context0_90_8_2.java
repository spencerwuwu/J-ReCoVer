

	Text vword = new Text();
	public void reduce(Text key, Iterable<DoubleWritable> values, Context context) throws IOException, InterruptedException
	{
		double min = Double.MAX_VALUE;
		double max = 0.0;
		for(DoubleWritable value : values)
		{
			double myvalue = value.get();
			max = (max > myvalue) ? max : myvalue;
			min = (min < myvalue) ? min : myvalue;
		}
		String minmax = "Min is " + min + ", Max is " + max;
		vword.set(minmax);
		context.write(key, vword);
	}

