    private IntWritable result = new IntWritable();

    public void reduce(Text key, Iterable<IntWritable> values, 
                       Context context
                       ) throws IOException, InterruptedException {
	
	
	
      int sum = 0;
      for (IntWritable val : values) {
        sum += val.get();
      }
      
	
		if(sum > 230 && (1 == 1))
		{
 			result.set(sum);
			context.write(key, result);
    	}
	
  }

