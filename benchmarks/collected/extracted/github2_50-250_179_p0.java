    
    /*[*/private NcdcStationMetadata metadata;/*]*/
    
    protected void setup(Context context)
        throws IOException, InterruptedException {
      metadata = new NcdcStationMetadata();
      metadata.initialize(new File("stations-fixed-width.txt"));
    }/*]*/

    public void reduce(Text key, Iterable<IntWritable> values,
        Context context) throws IOException, InterruptedException {
      
      /*[*/String stationName = metadata.getStationName(key.toString());/*]*/
      
      int maxValue = Integer.MIN_VALUE;
      for (IntWritable value : values) {
        maxValue = Math.max(maxValue, value.get());
      }
      context.write(new Text(/*[*/stationName/*]*/), new IntWritable(maxValue));
    }
