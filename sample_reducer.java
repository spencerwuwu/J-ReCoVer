//https://searchcode.com/file/73390634/src/Corrector/IdentifyTrustedReads.java#l-29
	private static long KmerThreshold = 1 ;
	public void reduce(Text prefix, Iterator<IntWritable> iter,
			OutputCollector<Text, IntWritable> output, Reporter reporter)
					throws IOException
	{
		int sum =0;
		while(iter.hasNext())
		{
		    sum += iter.next().get();
		}
		output.collect(prefix, new IntWritable(sum));
	}
