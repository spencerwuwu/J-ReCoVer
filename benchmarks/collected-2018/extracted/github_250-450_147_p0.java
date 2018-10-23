    protected final IntWritable one = new IntWritable(1);

    int srcs = 0;
    
    /*
    public void setup(Context context) {
      srcs = context.getConfiguration().getInt("testdatamerge.sources", 0);
      assertTrue("Invalid src count: " + srcs, srcs > 0);
    }
    */

    public void reduce(IntWritable key, Iterable<IntWritable> values,
        Context context) throws IOException, InterruptedException {
      int seen = 0;
      for (IntWritable value : values) {
        seen += value.get();
      }
      //assertTrue("Bad count for " + key.get(), verify(key.get(), seen));
      context.write(key, new IntWritable(seen));
    }
    
    //public abstract boolean verify(int key, int occ);
