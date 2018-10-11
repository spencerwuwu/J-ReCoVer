
    public void reduce(Text key, Iterator<IntWritable> values, OutputCollector<Text, IntWritable> output,
        Reporter reporter) throws IOException {
      output.collect(key, values.next());
    }

    public void configure(JobConf job) {

    }

    public void close() throws IOException {

    }
