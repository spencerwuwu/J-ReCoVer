    LongWritable longWritable = new LongWritable(0);

    public void reduce(Text key, Iterator<LongWritable> values,
                       OutputCollector<Text, LongWritable> collector,
                       Reporter reporter) throws IOException {

      long total = 0;
      while (values.hasNext()) {
        total += values.next().get();
      }

      longWritable.set(total);
      collector.collect(key, longWritable);
    }
