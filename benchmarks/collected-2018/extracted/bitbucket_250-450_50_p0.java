    
    private long numInside = 0;
    private long numOutside = 0;
      
    /** Store job configuration. */
    /*
    public void configure(JobConf job) {
      conf = job;
    }
    */

    /**
     * Accumulate number of points inside/outside results from the mappers.
     * @param isInside Is the points inside? 
     * @param values An iterator to a list of point counts
     * @param output dummy, not used here.
     * @param reporter
     */
    public void reduce(BooleanWritable isInside,
                       Iterator<LongWritable> values,
                       OutputCollector<Text, LongWritable> output,
                       Reporter reporter) throws IOException {
      if (isInside.get()) {
        for(; values.hasNext(); numInside += values.next().get());
      } else {
        for(; values.hasNext(); numOutside += values.next().get());
      }
    }

    /**
     * Reduce task done, write output to a file.
     */
/*
    public void close() throws IOException {
      //write output to a file
      Path outDir = new Path(TMP_DIR, "out");
      Path outFile = new Path(outDir, "reduce-out");
      FileSystem fileSys = FileSystem.get(conf);
      SequenceFile.Writer writer = SequenceFile.createWriter(fileSys, conf,
          outFile, LongWritable.class, LongWritable.class, 
          CompressionType.NONE);
      writer.append(new LongWritable(numInside), new LongWritable(numOutside));
      writer.close();
    }
    */
