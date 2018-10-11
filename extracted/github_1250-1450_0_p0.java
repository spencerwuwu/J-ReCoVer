
    @Override
    public void reduce(Text key, Iterator<IntWritable> values, OutputCollector<Text, IntWritable> output,
        Reporter reporter) throws IOException {
      output.collect(key, values.next());
    }

    @Override
    public void configure(JobConf job) {

    }

    @Override
    public void close() throws IOException {

    }
