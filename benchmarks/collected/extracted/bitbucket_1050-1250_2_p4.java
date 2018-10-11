		public void reduce (final IntWritable key, final Iterator<IntWritable> values, final OutputCollector<IntWritable, IntWritable> output, final Reporter reporter) throws IOException
		{
			int sum = 0;

			while (values.hasNext()) {
				int cur_count = values.next().get();

				sum += cur_count;
			}

			output.collect(key, new IntWritable(sum));
		}
