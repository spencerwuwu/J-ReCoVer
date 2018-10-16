

    public void reduce(IntWritable key, java.lang.Iterable<IntWritable>
      values, org.apache.hadoop.mapreduce.Reducer<IntWritable, IntWritable, WritableComparable, HCatRecord>.Context context)
      throws IOException, InterruptedException {
      int sum = 0;
      Iterator<IntWritable> iter = values.iterator();
      while (iter.hasNext()) {
        sum++;
        iter.next();
      }
      HCatRecord record = new DefaultHCatRecord(2);
      record.set(0, key.get());
      record.set(1, sum);

      context.write(null, record);
    }
