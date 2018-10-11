		public void reduce (final IntWritable key, final Iterator<DoubleWritable> values, final OutputCollector<IntWritable, DoubleWritable> output, final Reporter reporter) throws IOException
        {
			double sum = 0;
			int count = 0;

			while (values.hasNext()) {
				double cur_val = values.next().get();
				sum += cur_val;
				count++;
			}

			output.collect( key, new DoubleWritable( sum/count ) );
		}
