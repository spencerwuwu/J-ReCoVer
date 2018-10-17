    Set<String> stringsToKeep;
    public void setup(Context context)
    {
      stringsToKeep = new HashSet<String>();
      String cachedString = CacheUtils.readSerializableFromCache(context.getConfiguration(), "idfMap", String.class);
      for(String subString : cachedString.split(" "))
      {
        stringsToKeep.add(subString);
      }
    }
    
    public void reduce(Text key, Iterable<IntWritable> values, Context context) throws IOException, InterruptedException
    {
      int count = 0;
      if(stringsToKeep.contains(key.toString()))
      {
        for(IntWritable value : values) count += value.get();
      }
      context.write(key, new IntWritable(count));
    }
