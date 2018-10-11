
  public void reduce(K key, Iterator<LongWritable> values,
                     OutputCollector<K, LongWritable> output,
                     Reporter reporter)
    throws IOException {

    // sum all values for this key
    long sum = 0;
    while (values.hasNext()) {
      sum += values.next().get();
    }

    // output sum
    output.collect(key, new LongWritable(sum));
  }

