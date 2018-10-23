		public void reduce (final LongWritable key, final Iterator<DoubleWritable> values, final OutputCollector<LongWritable, Text> output, final Reporter reporter) throws IOException
        {
			int i;
			double next_rank = 0;

			while (values.hasNext()) {
				String cur_value_str = values.next().toString();
				next_rank += Double.parseDouble( cur_value_str ) ;
			}

			output.collect( key, new Text( "v" + next_rank ) );
		}
