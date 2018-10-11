    
    public void configure(JobConf job) {}

    // keep track of the last key we've seen
    private int lastKey = Integer.MIN_VALUE;
    public void reduce(IntWritable key, Iterator<Writable> values, 
                       OutputCollector<IntWritable, Text> out,
                       Reporter reporter) throws IOException {
      int currentKey = key.get();
      // keys should be in ascending order
      if (currentKey < lastKey) {
        fail("Keys not in sorted ascending order");
      }
      lastKey = currentKey;
      out.collect(key, new Text("success"));
    }
    
    public void close() {}
