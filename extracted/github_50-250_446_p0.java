		LongWritable vword = new LongWritable();
		public void reduce(Text key, Iterator<IntWritable> values,
				OutputCollector<Text, LongWritable> collector, Reporter reporter)
				throws IOException {
			long sum = 0L;
			while(values.hasNext())
			{
				sum = sum + values.next().get();
			}
			vword.set(sum);
			collector.collect(key, vword);
		}

