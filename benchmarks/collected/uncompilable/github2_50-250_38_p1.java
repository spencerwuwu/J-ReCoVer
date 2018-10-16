
    protected void setup(Context context) throws IOException, InterruptedException {
      Configuration conf = context.getConfiguration();
      String vocabulary_path = conf.getRaw("thrax.work-dir") + "vocabulary/part-*";
      Vocabulary.initialize(conf, vocabulary_path);
    }

    private int marginal;
    private FloatWritable prob;

    public void reduce(RuleWritable key, Iterable<IntWritable> values, Context context)
        throws IOException, InterruptedException {
      if (key.lhs == PrimitiveUtils.MARGINAL_ID) {
        // We only get here if it is the very first time we saw the source.
        marginal = 0;
        for (IntWritable x : values)
          marginal += x.get();
        return;
      }

      // Control only gets here if we are using the same marginal.
      if (Arrays.equals(key.source, PrimitiveArrayMarginalComparator.MARGINAL)) {
        // We only get in here if it's a new LHS.
        int count = 0;
        for (IntWritable x : values)
          count += x.get();
        prob = new FloatWritable((float) -Math.log(count / (float) marginal));
        return;
      }
      context.write(key, new FeaturePair(Vocabulary.id(LABEL), prob));
    }

