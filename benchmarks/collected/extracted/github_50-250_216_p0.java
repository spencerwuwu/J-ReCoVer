		private static final FloatWritable sumWritable = new FloatWritable();

		public void reduce(Text key, Iterable<FloatWritable> values, Context context) throws IOException, InterruptedException {
			int sum = 0;
			Iterator<FloatWritable> iter = values.iterator();
			while (iter.hasNext()) {
				sum += iter.next().get();
			}
			sumWritable.set(sum);
			context.write(key, sumWritable);
		}
