        private static final Text LABEL = new Text("RarityPenalty");

        public void reduce(RuleWritable key, Iterable<IntWritable> values, Context context) throws IOException, InterruptedException
        {
            int count = 0;
            for (IntWritable x : values)
                count += x.get();
            key.featureLabel.set(LABEL);
            key.featureScore.set(Math.exp(1 - count));
            context.write(key, NullWritable.get());
        }

