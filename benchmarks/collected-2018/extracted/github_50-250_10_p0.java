           public void reduce(Text key, Iterator<IntWritable> values, OutputCollector<Text, IntWritable> output, Reporter reporter) throws IOException {
              int sum = 0;
              while (values.hasNext()){
                 sum += values.next().get();
              }
              output.collect(key, new IntWritable(sum));
           }
