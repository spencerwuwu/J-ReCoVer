  
  public void reduce(IntPairWritable topicWord,
                     Iterable<DoubleWritable> values,
                     Context context) throws java.io.IOException, InterruptedException {
    
    // sum likelihoods
    if (topicWord.getSecond() == LDADriver.LOG_LIKELIHOOD_KEY) {
      double accum = 0.0;
      for (DoubleWritable vw : values) {
        double v = vw.get();
        Preconditions.checkArgument(!Double.isNaN(v),
                                    "Found NaN for topic=(%d,%d)", topicWord.getFirst(), topicWord.getSecond());
        accum += v;
      }
      context.write(topicWord, new DoubleWritable(accum));
    } else { // log sum sufficient statistics.
      double accum = Double.NEGATIVE_INFINITY;
      for (DoubleWritable vw : values) {
        double v = vw.get();
        Preconditions.checkArgument(!Double.isNaN(v),
                                    "Found NaN for topic = (%d,%d)", topicWord.getFirst(), topicWord.getSecond());
        accum = LDAUtil.logSum(accum, v);
        Preconditions.checkArgument(!Double.isNaN(accum),
                                    "Accumulated NaN for topic = (%d,%d)", topicWord.getFirst(), topicWord.getSecond());
      }
      context.write(topicWord, new DoubleWritable(accum));
    }
  }
