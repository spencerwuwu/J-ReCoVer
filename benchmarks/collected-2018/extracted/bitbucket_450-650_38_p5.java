		public void reduce (final Text key, final Iterator<DoubleWritable> values, final OutputCollector<Text, DoubleWritable> output, final Reporter reporter) throws IOException
        {
			double sum = 0;

			while (values.hasNext()) {
				double cur_val = values.next().get();
				sum += cur_val;
			}

			output.collect( key, new DoubleWritable( sum ) );
		}
