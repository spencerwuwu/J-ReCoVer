    
    private int nblabels;
    
    protected void setup(Context context) throws IOException, InterruptedException {
      Configuration conf = context.getConfiguration();
      Dataset dataset = Builder.loadDataset(conf);
      setup(dataset.nblabels());
    }
    
    /**
     * Useful when testing
     */
    protected void setup(int nblabels) {
      this.nblabels = nblabels;
    }
    
    public void reduce(LongWritable key, Iterable<IntWritable> values, Context context)
      throws IOException, InterruptedException {
      int[] counts = new int[nblabels];
      for (IntWritable value : values) {
        counts[value.get()]++;
      }
      
      context.write(key, new Frequencies(key.get(), counts));
    }
