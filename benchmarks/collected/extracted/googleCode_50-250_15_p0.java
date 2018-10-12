    
    /*[*/private NcdcStationMetadata metadata;/*]*/
    
    public void configure(JobConf conf) {
      metadata = new NcdcStationMetadata();
      try {
        metadata.initialize(new File("stations-fixed-width.txt"));
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }/*]*/

    public void reduce(Text key, Iterator<IntWritable> values,
        OutputCollector<Text, IntWritable> output, Reporter reporter)
        throws IOException {
      
      /*[*/String stationName = metadata.getStationName(key.toString());/*]*/
      
      int maxValue = Integer.MIN_VALUE;
      while (values.hasNext()) {
        maxValue = Math.max(maxValue, values.next().get());
      }
      output.collect(new Text(/*[*/stationName/*]*/), new IntWritable(maxValue));
    }
