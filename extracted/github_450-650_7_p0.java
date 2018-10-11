                         IntWritable, RecordStatsWritable> {
      
      public void reduce(IntWritable key, Iterator<RecordStatsWritable> values,
                         OutputCollector<IntWritable,
                                         RecordStatsWritable> output, 
                         Reporter reporter) throws IOException {
        long bytes = 0;
        long records = 0;
        int xor = 0;
        while (values.hasNext()) {
          RecordStatsWritable stats = values.next();
          bytes += stats.getBytes();
          records += stats.getRecords();
          xor ^= stats.getChecksum(); 
        }
        
        output.collect(key, new RecordStatsWritable(bytes, records, xor));
      }
