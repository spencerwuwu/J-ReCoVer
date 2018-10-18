/*
    private TopNScoredObjects<Integer> queue;

    public void setup(Reducer<IntWritable, FloatWritable, IntWritable, FloatWritable>.Context context)
        throws IOException {
      int k = context.getConfiguration().getInt("n", 100);
      queue = new TopNScoredObjects<Integer>(k);
    }
    */

    public void reduce(IntWritable nid, Iterable<FloatWritable> iterable, Context context)
        throws IOException {
      Iterator<FloatWritable> iter = iterable.iterator();
      //queue.add(nid.get(), iter.next().get());
      float f = iter.next().get();

      // Shouldn't happen. Throw an exception.
      if (iter.hasNext()) {
        //throw new RuntimeException();
        return;
      }
    }

/*
    public void cleanup(Reducer<IntWritable, FloatWritable, IntWritable, FloatWritable>.Context context)
        throws IOException, InterruptedException {
      IntWritable key = new IntWritable();
      FloatWritable value = new FloatWritable();

      for (PairOfObjectFloat<Integer> pair : queue.extractAll()) {
        key.set(pair.getLeftElement());
        value.set(pair.getRightElement());
        context.write(key, value);
      }
    }
    */
