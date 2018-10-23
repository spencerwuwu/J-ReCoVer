

	public void reduce(Text key, Iterator<DoubleWritable> values, OutputCollector<Text, DoubleWritable> output, Reporter reporter) throws IOException {
		double avgVol = 0;
		double sum = 0;
		double count = 0;
		while(values.hasNext()){
			sum += values.next().get();
			count++;
		}
		avgVol = sum/count;
		output.collect(key, new DoubleWritable(avgVol));
	}

