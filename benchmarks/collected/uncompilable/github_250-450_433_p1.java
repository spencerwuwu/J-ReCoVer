    private AvroKey<TextStats> mStats;
    private AvroMultipleOutputs amos;
    protected void setup(Context context) {
      mStats = new AvroKey<TextStats>(null);
      amos = new AvroMultipleOutputs(context);
    }

    public void reduce(Text line, Iterable<IntWritable> counts, Context context)
        throws IOException, InterruptedException {
      TextStats record = new TextStats();
      record.count = 0;
      for (IntWritable count : counts) {
        record.count += count.get();
      }
      record.name = line.toString();
      mStats.datum(record);
      context.write(mStats, NullWritable.get());
      amos.sync("myavro3","myavro3");
      amos.write("myavro3",mStats,NullWritable.get());
    }
    protected void cleanup(Context context) throws IOException,InterruptedException
    {
      amos.close();
    }
