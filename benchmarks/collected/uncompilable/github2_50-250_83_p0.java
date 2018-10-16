
    AvroKey<String> resultKey = new AvroKey<String>();
    AvroValue<Long> resultValue = new AvroValue<Long>();

    public void reduce(Text key, Iterable<LongWritable> values, Context context) throws IOException, InterruptedException {
      long sum = 0;
      for (LongWritable value: values) {
        sum += value.get();
      }
      resultKey.datum(key.toString());
      resultValue.datum(sum);

      context.write(resultKey, resultValue);
    }
