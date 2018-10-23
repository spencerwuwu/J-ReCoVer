		private final IntWritable SumValue = new IntWritable();

		public void reduce(Text keyIn, Iterable<IntWritable> values, Context context)
				throws IOException, InterruptedException {
			Iterator<IntWritable> iter = values.iterator();
			// sum values
			int sum = 0;
			while (iter.hasNext()) {
				sum += iter.next().get();
			}

			// keep original tuple key, emit sum of counts as value
			SumValue.set(sum);
			context.write(keyIn, SumValue);
		}
