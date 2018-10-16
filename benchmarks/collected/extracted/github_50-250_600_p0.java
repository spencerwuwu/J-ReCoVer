
		// reuse objects
		private final static IntWritable SumValue = new IntWritable();

		public void reduce(Text key, Iterable<IntWritable> values,
				Context context) throws IOException, InterruptedException {
			// sum up values
			Iterator<IntWritable> iter = values.iterator();
			int sum = 0;
			while (iter.hasNext()) {
				sum += iter.next().get();
			}
			SumValue.set(sum);
			context.write(key, SumValue);
		}
