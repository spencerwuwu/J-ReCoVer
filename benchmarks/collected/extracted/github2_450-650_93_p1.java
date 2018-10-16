      
      public void reduce(BytesWritable key, Iterator<IntWritable> values,
                         OutputCollector<BytesWritable, IntWritable> output,
                         Reporter reporter) throws IOException {
	IntWritable sortInput = new IntWritable(1);
	IntWritable sortOutput = new IntWritable(2);
        int ones = 0;
        int twos = 0;
        while (values.hasNext()) {
          IntWritable count = values.next(); 
          if (count.equals(sortInput)) {
            ++ones;
          } else if (count.equals(sortOutput)) {
            ++twos;
          } else {
            throw new IOException("Invalid 'value' of " + count.get() + 
                                  " for (key,value): " + key.toString());
          }
        }
        
        // Check to ensure there are equal no. of ones and twos
        if (ones != twos) {
          throw new IOException("Illegal ('one', 'two'): (" + ones + ", " + twos +
                                ") for (key, value): " + key.toString());
        }
      }
