  {
    public void reduce( Text key, Iterator<LongWritable> values, OutputCollector<Text, LongWritable> output, Reporter reporter)
      throws IOException
    {
      long sum = 0;

      while ( values.hasNext( ) )
        {
          LongWritable value = values.next( );
          
          sum += value.get( );
        }
      
      output.collect( key, new LongWritable( sum ) );
    }
