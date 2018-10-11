    private long topN;
    private long count = 0L;
    
    public void reduce(FloatWritable key, Iterator<Text> values, OutputCollector<FloatWritable, Text> output, Reporter reporter) throws IOException {
      while (values.hasNext() && count < topN) {
        key.set(-key.get());
        output.collect(key, values.next());
        count++;
      }
    }

    public void configure(JobConf job) {
      topN = job.getLong("db.reader.topn", 100) / job.getNumReduceTasks();
    }
    
    public void close() {}
