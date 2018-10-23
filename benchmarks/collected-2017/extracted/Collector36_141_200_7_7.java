

	public void reduce(LongWritable key, Iterator<Long> values,
	        OutputCollector<LongWritable, Long> output, Reporter reporter)
	      throws IOException {

	      long sum = 0;
	      while (values.hasNext()) {
	        sum += values.next();
	      }
	      output.collect(key, sum);
	    }
