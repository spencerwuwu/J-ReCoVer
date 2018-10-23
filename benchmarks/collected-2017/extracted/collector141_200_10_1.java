

	private long KmerThreshold = 1 ;

	public void reduce(Text prefix, Iterator<IntWritable> iter,
			OutputCollector<Text, IntWritable> output, Reporter reporter)
					throws IOException
	{
		int sum =0;
		int untrust_count = 0;
		int TRUST = 1;
		while(iter.hasNext())
		{
			int frequency = iter.next().get();
			if (frequency <= KmerThreshold) {
				untrust_count = untrust_count + 1;
				TRUST = 0;
				break;
			}
		}

		output.collect(prefix, new IntWritable(TRUST));
	}

