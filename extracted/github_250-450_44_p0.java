		IntWritable res = new IntWritable();
		public void reduce(PhrasePair key, Iterator<IntWritable> values,
				OutputCollector<PhrasePair,IntWritable> output, 
				Reporter reporter) throws IOException {
			int sum = 0;
			while (values.hasNext()) {
				sum += values.next().get();
			}
			res.set(sum);
			output.collect(key, res);
		}
