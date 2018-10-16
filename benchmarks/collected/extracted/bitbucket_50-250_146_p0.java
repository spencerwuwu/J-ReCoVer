    private static final Text SEPARATOR = 
      new Text("------------------------------------------------");
    private final Text first = new Text();
    
    public void reduce(Text key, Iterable<IntWritable> values,
                       Context context
                       ) throws IOException, InterruptedException {
      context.write(SEPARATOR, null);
      first.set(Integer.toString(key.getFirst()));
      for(IntWritable value: values) {
        context.write(first, value);
      }
    }
