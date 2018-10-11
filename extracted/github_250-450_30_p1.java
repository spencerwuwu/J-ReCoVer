
		@Override
		public void reduce(IntWritable k, Iterator<IntWritable> v, OutputCollector<IntWritable, IntWritable> out, Reporter r)
				throws IOException {
			while(v.hasNext()) {
				out.collect(new IntWritable(k.get() % 4), v.next());
			}
		}
		
		@Override
		public void configure(JobConf arg0) { }

		@Override
		public void close() throws IOException { }
