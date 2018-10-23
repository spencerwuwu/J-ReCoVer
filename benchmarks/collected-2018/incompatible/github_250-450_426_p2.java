
    private int numFeatures;
    private final Random random = RandomUtils.getRandom();

    protected void setup(Context ctx) throws IOException, InterruptedException {
      super.setup(ctx);
      numFeatures = ctx.getConfiguration().getInt(NUM_FEATURES, -1);

      Preconditions.checkArgument(numFeatures > 0, "numFeatures was not set correctly!");
    }

    public void reduce(VarLongWritable itemID, Iterable<FloatWritable> ratings, Context ctx) 
        throws IOException, InterruptedException {

      RunningAverage averageRating = new FullRunningAverage();
      for (FloatWritable rating : ratings) {
        averageRating.addDatum(rating.get());
      }

      int itemIDIndex = TasteHadoopUtils.idToIndex(itemID.get());
      Vector columnOfM = new DenseVector(numFeatures);

      columnOfM.setQuick(0, averageRating.getAverage());
      for (int n = 1; n < numFeatures; n++) {
        columnOfM.setQuick(n, random.nextDouble());
      }

      ctx.write(new VarIntWritable(itemIDIndex), new FeatureVectorWithRatingWritable(itemIDIndex, columnOfM));
    }
