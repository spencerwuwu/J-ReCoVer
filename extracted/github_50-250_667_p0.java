
  @Override
  public void reduce(Text key,
                     Iterator<DoubleWritable> values,
                     OutputCollector<Text, DoubleWritable> output,
                     Reporter reporter) throws IOException {
    //Key is label,word, value is the tfidf of the feature  of times we've seen this label word per local node.  Output is the same

    double sum = 0.0;
    while (values.hasNext()) {
      sum += values.next().get();
    }
    output.collect(key, new DoubleWritable(sum));
  }
