		public void reduce (final IntWritable key, final Iterator<IntWritable> values, final OutputCollector<IntWritable, IntWritable> output, final Reporter reporter) throws IOException
        {
			int sum = 0;

			while (values.hasNext()) {
				int cur_value = values.next().get();
				sum += cur_value;
			}

			output.collect( key, new IntWritable(sum) );
		}
