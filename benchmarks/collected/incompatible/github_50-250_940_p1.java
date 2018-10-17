
    private int marginal;

    protected void setup(Context context) throws IOException, InterruptedException {
      Configuration conf = context.getConfiguration();
      String vocabulary_path = conf.getRaw("thrax.work-dir") + "vocabulary/part-*";
      Vocabulary.initialize(conf, vocabulary_path);
    }

    public void reduce(RuleWritable key, Iterable<IntWritable> values, Context context)
        throws IOException, InterruptedException {
      if (Arrays.equals(key.target, PrimitiveArrayMarginalComparator.MARGINAL)) {
        // we only get here if it is the very first time we saw the LHS
        // and source combination
        marginal = 0;
        for (IntWritable x : values)
          marginal += x.get();
        return;
      }

      // control only gets here if we are using the same marginal
      int count = 0;
      for (IntWritable x : values)
        count += x.get();

      FloatWritable prob = new FloatWritable((float) -Math.log(count / (float) marginal));
      context.write(key, new FeaturePair(Vocabulary.id(LABEL), prob));
    }

