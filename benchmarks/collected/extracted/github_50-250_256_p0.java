
        public void reduce(Text key, Iterator<LongWritable> values, OutputCollector<Text, LongWritable> output, Reporter reporter)
                throws IOException {
            long total = 0;
            while (values.hasNext()) {
                total += values.next().get();
                //output.collect(key, values.next());
            }
            output.collect(key, new LongWritable(total));
        }
