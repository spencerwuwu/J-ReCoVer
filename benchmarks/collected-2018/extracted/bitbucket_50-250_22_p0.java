    private DoubleWritable result = new DoubleWritable();

    public void reduce(IntWritable key, Iterable<DoubleWritable> values, 
                       Context context
                       ) throws IOException, InterruptedException {
      double sum = 0.0;
      //byte[] dataArray = new byte[8];
      for (DoubleWritable val : values) {
    	  
    	  sum = Math.max(sum,val.get());
        //val.get();
      }
      System.out.println("Result of reduce is "+sum);
      //EndianUtils.writeSwappedDouble(dataArray, 0, sum);
      result.set(sum);
      //result.set(sum);
      context.write(new IntWritable(1), result);
    }
