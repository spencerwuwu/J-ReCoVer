

	private IntWritable result = new IntWritable();

	public void reduce(Text key, Iterable<IntWritable> values, 
			Context context
			) throws IOException, InterruptedException {
		// since the mapper output and reducer outputs are of same type we cna keep the key value types same
		// we will filter keys and eleminate any records that has word Berners-Lee or whos total vlaues are less then min value

		int sum = 0;
		for (IntWritable val : values) {
			sum += val.get();
		}
		result.set(sum);
		// access runtime parameter for min value
		//Configuration conf = context.getConfiguration();
		int min=0;
		// filter for a value, 
		if((key.toString().equals("Berners-Lee")==false) || sum>min ) context.write(key, result);
	}

