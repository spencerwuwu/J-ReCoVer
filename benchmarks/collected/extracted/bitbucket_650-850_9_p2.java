		int number_nodes = 0;
		double mixing_c = 0;
		double random_coeff = 0;

		public void configure(JobConf job) {
			number_nodes = Integer.parseInt(job.get("number_nodes"));
			mixing_c = Double.parseDouble(job.get("mixing_c"));
			random_coeff = (1-mixing_c) / (double)number_nodes;

			System.out.println("RedStage2: number_nodes = " + number_nodes + ", mixing_c = " + mixing_c + ", random_coeff = " + random_coeff );
		}

		public void reduce (final IntWritable key, final Iterator<DoubleWritable> values, final OutputCollector<IntWritable, DoubleWritable> output, final Reporter reporter) throws IOException
        {
			int i;
			double min_value = 1.0;
			double max_value = 0.0;

			int min_or_max = key.get();	// 0 : min, 1: max

			while (values.hasNext()) {
				double cur_value = values.next().get();

				if( min_or_max == 0 ) {	// find min
					if( cur_value < min_value )
						min_value = cur_value;
				} else {				// find max
					if( cur_value > max_value )
						max_value = cur_value;
				}
			}

			if( min_or_max == 0)
				output.collect( key, new DoubleWritable(min_value) );
			else
				output.collect( key, new DoubleWritable(max_value) );
		}
