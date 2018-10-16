
  private int minSupport = 0;

  public void reduce(Text key, Iterable<LongWritable> values, Context context) throws IOException, InterruptedException {
    long sum = 0;
    for (LongWritable value : values) {
      sum += value.get();
    }
    if (sum >= minSupport) {
      context.write(key, new LongWritable(sum));
    }
  }

/*
  protected void setup(Context context) throws IOException, InterruptedException {
    super.setup(context);
    minSupport = context.getConfiguration().getInt(DictionaryVectorizer.MIN_SUPPORT, DictionaryVectorizer.DEFAULT_MIN_SUPPORT);
  }
  */

