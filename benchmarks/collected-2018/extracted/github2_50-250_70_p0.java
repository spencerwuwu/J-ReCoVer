
    public void reduce(Text key, Iterable<LongWritable> values, Context context) throws IOException, InterruptedException {
      long summ = 0;
      for (LongWritable value : values) {
        summ += value.get();
      }

      context.write(key, new LongWritable(summ));
    }
