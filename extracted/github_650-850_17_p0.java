                     WritableComparable, Writable> {
    static final String TARGET_VALUE = "reduce.memory-loader.target-value";
    private static MemoryLoader loader = null;
    
    public void reduce(WritableComparable key, Iterator<Writable> val, 
                       OutputCollector<WritableComparable, Writable> output,
                       Reporter reporter)
    throws IOException {
      assertNotNull("Reducer not configured!", loader);
      
      // load the memory
      loader.load();
      
      // work as identity reducer
      output.collect(key, key);
    }

    public void configure(JobConf conf) {
      loader = new MemoryLoader(conf.getLong(TARGET_VALUE, -1));
    }
