        public void reduce(Text key, Iterable<IntWritable> values,
                Context context)
                throws IOException, InterruptedException {
            int sum = 0;
            while (values.hasNext()) {
                sum += values.next().get();
            }
            context.write(key, new IntWritable(sum));
        }
