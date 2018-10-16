		public void reduce(Text key, Iterable<LongWritable> values, Context context) throws IOException, InterruptedException
		{
			long sum = 0;
			for(LongWritable value : values)
			{
				sum = sum + value.get();
			}
			//String country = context.getConfiguration().get("country");
			String country = "_";
			String record = country + ": " + key.toString(); 
			context.write(new Text(record), new LongWritable(sum));
		}
