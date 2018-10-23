		public void reduce (final IntWritable key, final Iterator<IntWritable> values, OutputCollector<IntWritable, IntWritable> output, final Reporter reporter) throws IOException
        {
			int count = 0;

			while (values.hasNext()) {
				int cur_count = values.next().get();
				count += cur_count;
			}

			IntWritable count_int = new IntWritable(count);
			output.collect(key, count_int );
		}
