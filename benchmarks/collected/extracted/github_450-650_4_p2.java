    public void reduce(IntWritable key, Iterable<IntWritable> it,
        Context context) throws IOException, InterruptedException {
      int keyint = key.get();
      int total = 0;
      while (it.hasNext()) {
        total += it.next().get();
      }
      context.write(new IntWritable(keyint), new IntWritable(total));
    }
