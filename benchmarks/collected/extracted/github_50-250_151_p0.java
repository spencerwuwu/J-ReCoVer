		/**
		 * Reduce method which implements summation. Acts as both reducer and
		 * combiner.
		 * 
		 * @throws IOException
		 */
		public void reduce(Text key, Iterable<IntWritable> values, Context context)
		throws IOException, InterruptedException {
			int sum = 0;
			Iterator<IntWritable> iterator = values.iterator();
			while (iterator.hasNext()) {
				sum += iterator.next().get();
			}
			context.write(key, new IntWritable(sum));
		}
