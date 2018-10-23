		public void reduce(Text key, Iterator<DoubleWritable> values, OutputCollector<Text, DoubleWritable> output, Reporter reporter) throws IOException {
			double avgVal = 0;
			int count = 0;
			double sum = 0;
			while(values.hasNext()){
				sum += values.next().get();
				count++;
			}
			avgVal = sum/count;
			output.collect(key, new DoubleWritable(avgVal));
		}
