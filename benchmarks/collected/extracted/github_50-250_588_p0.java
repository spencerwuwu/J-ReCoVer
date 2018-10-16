    private final static IntWritable cnt = new IntWritable(1);

    public void reduce(Text key, Iterable<IntWritable> values, Context context)
        throws IOException, InterruptedException {
      context.write(key, cnt);
      cnt.set(cnt.get() + 1);
    }
