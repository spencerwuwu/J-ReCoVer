    LongWritable val = new LongWritable();
    
    //public CrawlDbStatCombiner() { }
    public void configure(JobConf job) { }
    public void close() {}
    public void reduce(Text key, Iterator<LongWritable> values, OutputCollector<Text, LongWritable> output, Reporter reporter)
        throws IOException {
      val.set(0L);
      String k = ((Text)key).toString();
      if (!k.equals("s")) {
        while (values.hasNext()) {
          LongWritable cnt = (LongWritable)values.next();
          val.set(val.get() + cnt.get());
        }
        output.collect(key, val);
      } else {
        long total = 0;
        long min = Long.MAX_VALUE;
        long max = Long.MIN_VALUE;
        while (values.hasNext()) {
          LongWritable cnt = (LongWritable)values.next();
          if (cnt.get() < min) min = cnt.get();
          if (cnt.get() > max) max = cnt.get();
          total += cnt.get();
        }
        output.collect(new Text("scn"), new LongWritable(min));
        output.collect(new Text("scx"), new LongWritable(max));
        output.collect(new Text("sct"), new LongWritable(total));
      }
    }
