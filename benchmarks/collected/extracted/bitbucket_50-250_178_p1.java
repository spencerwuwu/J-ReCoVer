    public void reduce(Text key, Iterable<LongWritable> values, Context context) throws IOException, InterruptedException {
      long total = 0;

      for (LongWritable val : values) {
        total += val.get();
      }
      context.write(key, new LongWritable(total));
    }
