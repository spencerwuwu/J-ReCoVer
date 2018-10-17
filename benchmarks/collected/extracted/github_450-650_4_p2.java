    public void reduce(IntWritable key, Iterable<IntWritable> it,
        Context context) throws IOException, InterruptedException {
      int keyint = key.get();
      int total = 0;
      Iterator<IntWritable> iter = it.iterator();
      while (iter.hasNext()) {
        total += iter.next().get();
      }
      context.write(new IntWritable(keyint), new IntWritable(total));
    }
