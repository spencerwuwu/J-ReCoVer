		public void reduce (final IntWritable key, final Iterator<IntWritable> values, OutputCollector<IntWritable, IntWritable> output, final Reporter reporter) throws IOException
        {
			int count = 0;

			while (values.hasNext()) {
				int cur_count = values.next().get();
				count += cur_count;
			}

			output.collect(key, new IntWritable(count) );
		}
