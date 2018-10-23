  
    int numSeen;
    int actualSum;
    public void configure(JobConf job) { }

    public void reduce(IntWritable key, Iterator<IntWritable> val,
                       OutputCollector<IntWritable, IntWritable> out,
                       Reporter reporter) throws IOException {
      actualSum += key.get(); // keep the running count of the seen values
      numSeen++; // number of values seen so far
      
      // using '1+2+3+...n =  n*(n+1)/2' to validate
      int expectedSum = numSeen * (numSeen + 1) / 2;
      if (expectedSum != actualSum) {
        throw new IOException("Collect test failed!! Ordering mismatch.");
      }
    }

    public void close() { }
