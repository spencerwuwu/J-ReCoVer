  //if true, then output weight
  private boolean useCounts = true;
  /**
   * We can either ignore how many times the user interacted (boolean) or output the number of times they interacted.
   */
  public static final String USE_COUNTS_PREFERENCE = "useBooleanPreferences";

  /*
  protected void setup(Context context) throws IOException, InterruptedException {
    useCounts = context.getConfiguration().getBoolean(USE_COUNTS_PREFERENCE, true);
  }
  */

  public void reduce(Text key, Iterable<LongWritable> values, Context context)
    throws IOException, InterruptedException {
    if (useCounts) {
      long sum = 0;
      for (LongWritable value : values) {
        sum++;
      }
      context.write(new Text(key.toString() + ',' + sum), null);
    } else {
      context.write(new Text(key.toString()), null);
    }
  }
