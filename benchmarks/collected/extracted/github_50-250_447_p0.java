		LongWritable vword = new LongWritable();
		public void reduce(Text key, Iterable<LongWritable> values, Context context) throws IOException, InterruptedException
		{
			long count = 0;
			for(LongWritable value : values)
			{
				count = count + value.get();
			}
			vword.set(count);
			context.write(key, vword);
		}
