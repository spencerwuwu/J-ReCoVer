    public void configure(JobConf job) {}
    public void close() {}
    public void reduce(Text key, Iterator<LongWritable> values, OutputCollector<Text, LongWritable> output, Reporter reporter)
            throws IOException {

      String k = ((Text) key).toString();
      if (k.equals("T")) {
        // sum all values for this key
        long sum = 0;
        while (values.hasNext()) {
          sum += ((LongWritable) values.next()).get();
        }
        // output sum
        output.collect(key, new LongWritable(sum));
      } else if (k.startsWith("status") || k.startsWith("retry")) {
        LongWritable cnt = new LongWritable();
        while (values.hasNext()) {
          LongWritable val = (LongWritable)values.next();
          cnt.set(cnt.get() + val.get());
        }
        output.collect(key, cnt);
      } else if (k.equals("scx")) {
        LongWritable cnt = new LongWritable(Long.MIN_VALUE);
        while (values.hasNext()) {
          LongWritable val = (LongWritable)values.next();
          if (cnt.get() < val.get()) cnt.set(val.get());
        }
        output.collect(key, cnt);
      } else if (k.equals("scn")) {
        LongWritable cnt = new LongWritable(Long.MAX_VALUE);
        while (values.hasNext()) {
          LongWritable val = (LongWritable)values.next();
          if (cnt.get() > val.get()) cnt.set(val.get());
        }
        output.collect(key, cnt);
      } else if (k.equals("sct")) {
        LongWritable cnt = new LongWritable();
        while (values.hasNext()) {
          LongWritable val = (LongWritable)values.next();
          cnt.set(cnt.get() + val.get());
        }
        output.collect(key, cnt);
      }
    }
