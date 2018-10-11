		public void reduce(Text key, Iterator<IntWritable> values, OutputCollector<Text, IntWritable> output, Reporter reporter) throws IOException {
	            if (key.toString().equals("Haze")) {
	                int sum = 0;
	                while (values.hasNext()) {
	                    sum += values.next().get();
	                }
	                output.collect(key, new IntWritable(sum));
	            }
		}
