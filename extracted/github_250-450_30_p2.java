		private String countPrefix;
		
		@Override
		public void reduce(IntWritable k, Iterator<Text> vs, OutputCollector<IntWritable, IntWritable> out, Reporter r)
				throws IOException {
			int commentCnt = 0;
			while(vs.hasNext()) {
				String v = vs.next().toString();
				if(v.startsWith(this.countPrefix)) {
					commentCnt++;
				}
			}
			out.collect(k, new IntWritable(commentCnt));
		}
		
		@Override
		public void configure(final JobConf c) { 
			this.countPrefix = c.get("my.cntPrefix");
		}

		@Override
		public void close() throws IOException { }
