		public void reduce (final IntWritable key, final Iterator<DoubleWritable> values, final OutputCollector<IntWritable, DoubleWritable> output, final Reporter reporter) throws IOException
        {
			int i = 0;
			double val_double[] = new double[2];
			val_double[0] = 0;
			val_double[1] = 0;

			while (values.hasNext()) {
				val_double[i] = values.next().get();

				i++;
			}

			double result = val_double[0] + val_double[1];
			if( result != 0 )
				output.collect(key, new DoubleWritable(result));
		}
