    public void reduce(Text pair, Iterable<DoubleWritable> values, Context ctx)
        throws IOException, InterruptedException {
      ctx.write(pair, values.iterator().next());
    }
