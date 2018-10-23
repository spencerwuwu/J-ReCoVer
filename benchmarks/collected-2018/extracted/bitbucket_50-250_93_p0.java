
    private Text result = new Text();

    public void reduce(Text key, Iterable<IntWritable> values, Context context)	throws IOException, InterruptedException {
      int sum = 0;
      int totalDocs = 4076*2;
      for (IntWritable val : values){
        sum += val.get();
      }
      Float idf=new Float(Math.log(totalDocs/sum));
      result.set(idf.toString());
      context.write(key, result);
    }
