    public void reduce(DoubleWritable key, Iterable<DoubleWritable> ratingAndEstimate, Context ctx)
        throws IOException, InterruptedException {

      double error = Double.NaN;
      boolean bothFound = false;
      for (DoubleWritable ratingOrEstimate : ratingAndEstimate) {
        if (Double.isNaN(error)) {
          error = ratingOrEstimate.get();
        } else {
          error -= ratingOrEstimate.get();
          bothFound = true;
          break;
        }
      }

      if (bothFound) {
        ctx.write(new DoubleWritable(error), NullWritable.get());
      }
    }
