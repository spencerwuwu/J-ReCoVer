    private IntWritable result = new IntWritable();
  
    public void reduce(Text key, Iterable<IntWritable> values, 
                       Context context) throws IOException {
      int sum = 0;
      for (IntWritable val : values) {
        sum += val.get();
      }
      result.set(sum);
      context.write(key, result);
    }
