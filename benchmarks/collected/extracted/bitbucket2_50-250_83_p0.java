
  private static final double LOG_2 = Math.log(2.0);

  private final DoubleWritable result = new DoubleWritable();
  private long numberItems = 0;

  /*
  protected void setup(Context context) throws IOException, InterruptedException {
    super.setup(context);
    numberItems = Long.parseLong(context.getConfiguration().get(Entropy.NUMBER_ITEMS_PARAM));
  }
  */

  public void reduce(NullWritable key, Iterable<DoubleWritable> values, Context context)
      throws IOException, InterruptedException {
    double entropy = 0.0;
    for (DoubleWritable value : values) {
      entropy += value.get();
    }
    result.set((Math.log(numberItems) - entropy / numberItems) / LOG_2);
    context.write(key, result);
  }

