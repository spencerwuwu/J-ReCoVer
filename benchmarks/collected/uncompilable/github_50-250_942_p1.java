    private int marginal;
    private FloatWritable prob;

    protected void setup(Context context) throws IOException, InterruptedException {
      Configuration conf = context.getConfiguration();
      String vocabulary_path = conf.getRaw("thrax.work-dir") + "vocabulary/part-*";
      Vocabulary.initialize(conf, vocabulary_path);
    }

    public void reduce(RuleWritable key, Iterable<IntWritable> values, Context context)
        throws IOException, InterruptedException {
      if (Arrays.equals(key.source, PrimitiveArrayMarginalComparator.MARGINAL)) {
        marginal = 0;
        for (IntWritable x : values)
          marginal += x.get();
        return;
      }
      if (key.lhs == PrimitiveUtils.MARGINAL_ID) {
        int count = 0;
        for (IntWritable x : values)
          count += x.get();
        prob = new FloatWritable((float) -Math.log(count / (float) marginal));
        return;
      }
      context.write(key, new FeaturePair(Vocabulary.id(LABEL), prob));
    }

