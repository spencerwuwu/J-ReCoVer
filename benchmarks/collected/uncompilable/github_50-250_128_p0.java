
    public void reduce(Text key, Iterable<IntWritable> values,
                       Context context)
      throws IOException, InterruptedException {
      int sum = 0;
      for (IntWritable value : values) {
        sum += value.get();
      }
      context.write(new AvroWrapper<Pair<CharSequence, Integer>>
                    (new Pair<CharSequence, Integer>(key.toString(), sum)),
                    NullWritable.get());
    }
