                       AvroWrapper<Pair<CharSequence, Integer>>, NullWritable> {

    public void reduce(Text key, Iterator<IntWritable> values,
        OutputCollector<AvroWrapper<Pair<CharSequence, Integer>>, NullWritable> output,
        Reporter reporter) throws IOException {
      int sum = 0;
      while (values.hasNext()) {
        sum += values.next().get();
      }
      output.collect(new AvroWrapper<Pair<CharSequence, Integer>>(
          new Pair<CharSequence, Integer>(key.toString(), sum)),
          NullWritable.get());
    }
