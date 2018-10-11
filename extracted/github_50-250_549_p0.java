    
    private NcdcStationMetadata metadata;
    
    @Override
    public void configure(JobConf conf) {
      metadata = new NcdcStationMetadata();
      try {
        Path[] localPaths = DistributedCache.getLocalCacheFiles(conf);
        if (localPaths.length == 0) {
          throw new FileNotFoundException("Distributed cache file not found.");
        }
        File localFile = new File(localPaths[0].toString());
        metadata.initialize(localFile);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    public void reduce(Text key, Iterator<IntWritable> values,
        OutputCollector<Text, IntWritable> output, Reporter reporter)
        throws IOException {
      
      String stationName = metadata.getStationName(key.toString());
      
      int maxValue = Integer.MIN_VALUE;
      while (values.hasNext()) {
        maxValue = Math.max(maxValue, values.next().get());
      }
      output.collect(new Text(stationName), new IntWritable(maxValue));
    }
