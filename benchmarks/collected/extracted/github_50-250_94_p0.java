
    public void configure(JobConf job) {
      // TODO Auto-generated method stub
      
    }

    public void close() throws IOException {
      // TODO Auto-generated method stub
      
    }

    public void reduce(Text key, Iterator<IntWritable> values,OutputCollector<TextBytes, IntWritable> output, Reporter reporter)
        throws IOException {
      int count = 0;
      while (values.hasNext()) 
        count += values.next().get();
      if (count >= 10) { 
        output.collect(key, new IntWritable(count));
      }
    } 
    
