

	  public void reduce(Text key, Iterable<LongWritable> values, Context context) throws IOException, InterruptedException {
	        long sum = 0;
	        for (LongWritable v : values) {
	            sum += v.get();
	        }
	        context.write(key, new LongWritable(sum));
	    }

