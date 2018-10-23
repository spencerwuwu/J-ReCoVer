    private AvroKey<GenericData.Record> mStats;
    private AvroMultipleOutputs amos;

    protected void setup(Context context) {
      mStats = new AvroKey<GenericData.Record>(null);
      amos = new AvroMultipleOutputs(context);
    }

    public void reduce(Text line, Iterable<IntWritable> counts, Context context)
        throws IOException, InterruptedException {
      GenericData.Record record = new GenericData.Record(STATS_SCHEMA);
      GenericData.Record record2 = new GenericData.Record(STATS_SCHEMA_2);
      int sum = 0;
      for (IntWritable count : counts) {
        sum += count.get();
      }
      record.put("name", new Utf8(line.toString()));
      record.put("count", new Integer(sum));
      mStats.datum(record);
      context.write(mStats, NullWritable.get());
      amos.sync("myavro","myavro");
      amos.write("myavro",mStats,NullWritable.get());
      record2.put("name1", new Utf8(line.toString()));
      record2.put("count1", new Integer(sum));
      mStats.datum(record2);
      amos.write(mStats, NullWritable.get(), STATS_SCHEMA_2, null, "testnewwrite2");
      amos.sync("myavro1","myavro1");
      amos.write("myavro1",mStats);
      amos.write(mStats, NullWritable.get(), STATS_SCHEMA, null, "testnewwrite");
      amos.write(mStats, NullWritable.get(), "testwritenonschema");
    }

    protected void cleanup(Context context) throws IOException,InterruptedException
    {
      amos.close();
    }
