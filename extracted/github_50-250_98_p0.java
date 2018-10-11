    {
      public void reduce(Text key, Iterator<IntWritable> values, OutputCollector<WordCountInfoRecord, NullWritable> output, Reporter reporter) throws IOException
      {
        int sum = 0;
        while (values.hasNext()) {
          sum += values.next().get();
        }
        // Output Data into MySQL
        output.collect(new WordCountInfoRecord(key.toString(),sum), NullWritable.get());
      }
