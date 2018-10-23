
    private AvroKey<GenericData.Record> result ;

    protected void setup(Context context) {
      result = new AvroKey<GenericData.Record>();
      result.datum(new Record(Pair.getPairSchema(STRING,LONG)));
    }

    public void reduce(Text key, Iterable<LongWritable> values, Context context) throws IOException, InterruptedException {
      long count = 0;
      for (LongWritable value: values) {
        count += value.get();
      }

      result.datum().put("key", key.toString());
      result.datum().put("value", count);

      context.write(result, NullWritable.get());
    }
