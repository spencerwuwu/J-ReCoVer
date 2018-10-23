		public void reduce(Text key, Iterator<IntWritable> values, OutputCollector<Text, IntWritable> output, Reporter reporter) throws IOException {
			int maxTemp = 0;
			while(values.hasNext()){
				int nextTemp = values.next().get();
				if (nextTemp > maxTemp)
					maxTemp = nextTemp;
			}
			output.collect(key, new IntWritable(maxTemp));
		}
