		
		public void reduce (Text key, Iterator<IntWritable> values,
				OutputCollector<Text, IntWritable> output, Reporter report)
				throws IOException {
			
			int count = 0;
			
			while (values.hasNext()) {
				count += values.next().get();
			}
			
			output.collect(key, new IntWritable(count));
			
		}
