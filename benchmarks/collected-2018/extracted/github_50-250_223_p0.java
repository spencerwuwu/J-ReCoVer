
		public void reduce(Text k, Iterator<LongWritable> vs, OutputCollector<Text, LongWritable> out, Reporter rep)
				throws IOException {
			
			long cnt = 0;
			while(vs.hasNext()) {
				cnt += vs.next().get();
			}
			out.collect(k, new LongWritable(cnt));
			
		}
		
		public void configure(JobConf arg0) { }
		
		public void close() throws IOException { }
