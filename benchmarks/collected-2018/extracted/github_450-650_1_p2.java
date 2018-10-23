    
    public void configure(JobConf job) {
    }

    // keep track of the last key we've seen
    private int lastKey = Integer.MIN_VALUE;
    public void reduce(IntWritable key,
                       Iterator<IntWritable> values,
                       OutputCollector<IntWritable, Text> out,
                       Reporter reporter) throws IOException {
      // check key order
      int currentKey = key.get();
      if (currentKey < lastKey) {
      }
      lastKey = currentKey;
      // check order of values
      IntWritable previous = new IntWritable(Integer.MIN_VALUE);
      int valueCount = 0;
      while (values.hasNext()) {
        IntWritable current = values.next();
        
        // Check that the values are sorted
        if (current.compareTo(previous) < 0)
        previous = current;
        ++valueCount;
      }
      if (valueCount != 5) {
      }
      out.collect(key, new Text("success"));
    }

    public void close() {
    }
