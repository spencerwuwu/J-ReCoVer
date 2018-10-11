	{
		public void reduce(Text key, Iterator<LongWritable> values,
				OutputCollector<Text, Text> collect, Reporter reporter)
						throws IOException {
			long sum = 0;
			int counter = 0;
			while(values.hasNext())
			{
				sum = sum + values.next().get();
				counter++;
			}
			float avg = (float) sum/counter;
			String emitValue = sum + "\t" + avg;
			collect.collect(key, new Text(emitValue));
		}
