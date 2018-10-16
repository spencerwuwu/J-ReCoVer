    
    public void reduce(IntWritable key, Iterable<IntWritable> it,
        Context context) throws IOException, InterruptedException {
      for (IntWritable iw : it) {
        context.write(iw, null);
      }
    }
