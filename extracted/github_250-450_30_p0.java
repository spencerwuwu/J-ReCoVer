
		@Override
		public void reduce(IntWritable k, Iterator<IntWritable> v, OutputCollector<IntWritable, IntWritable> out, Reporter r)
				throws IOException {
			
			int sum = 0;
			while(v.hasNext()) {
				sum += v.next().get();
			}
			out.collect(k, new IntWritable(sum));
		}
		
		@Override
		public void configure(JobConf arg0) { }

		@Override
		public void close() throws IOException { }
