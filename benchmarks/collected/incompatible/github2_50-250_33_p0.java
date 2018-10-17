
        static enum ReduceCounters {
            UNIQUE_WORDS_COUNTED
        }

        private IntWritable count = new IntWritable();

        public void reduce(Text key, Iterable<IntWritable> values, Context context)
                throws IOException, InterruptedException {

            int sum = 0;
            for (IntWritable value : values) {
                sum += value.get();
            }
            count.set(sum);
            context.write(key, count);
            context.getCounter(ReduceCounters.UNIQUE_WORDS_COUNTED).increment(1L);
        }
