
    @Override
    public void reduce(Text key, Iterator<IntWritable> values, OutputCollector<LongWritable, Text> output,
        Reporter reporter) throws IOException {
      long fingerprint = URLFingerprint.generate64BitURLFPrint(key.toString());
      output.collect(new LongWritable(fingerprint), key);
    }

    @Override
    public void configure(JobConf job) {
    }

    @Override
    public void close() throws IOException {
    }

