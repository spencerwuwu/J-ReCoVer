        private int marginal;
        private static final Text NAME = new Text("SourcePhraseGivenTarget");

        public void reduce(RuleWritable key, Iterable<IntWritable> values, Context context) throws IOException, InterruptedException
        {
            if (key.source.equals(WordLexicalProbabilityCalculator.MARGINAL)) {
                marginal = 0;
                for (IntWritable x : values)
                    marginal += x.get();
                return;
            }
            
            // control only gets here if we are using the same marginal
            int count = 0;
            for (IntWritable x : values) {
                count += x.get();
            }
            key.featureLabel.set(NAME);
            key.featureScore.set(-Math.log(count / (double) marginal));
            context.write(key, NullWritable.get());
        }

