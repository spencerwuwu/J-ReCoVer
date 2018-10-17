
    public void reduce(IntWritable key, Iterable<IntWritable> values, 
        Context context) throws IOException, InterruptedException {

      int errors = 0;

      MarkableIterator<IntWritable> mitr = 
        new MarkableIterator<IntWritable>(values.iterator());

      switch (key.get()) {
      case 0:
        errors += test0(key, mitr);
        break;
      case 1:
        errors += test1(key, mitr);
        break;
      case 2:
        errors += test2(key, mitr);
        break;
      case 3:
        errors += test3(key, mitr);
        break;
      default:
        break;
      }
      context.write(key, new IntWritable(errors));
    }
