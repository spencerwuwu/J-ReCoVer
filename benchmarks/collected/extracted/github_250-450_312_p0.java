        private Text current = new Text();
        private int marginalCount = 2;

        public void reduce(Text key, Iterable<IntWritable> values, Context context) throws IOException, InterruptedException
        {
		/*
            if (!key.fst.equals(current)) {
                if (!key.snd.equals(MARGINAL))
                    return;
                current.set(key.fst);
                marginalCount = 0;
                for (IntWritable x : values)
                    marginalCount += x.get();
                return;
            }
	    */
            // control only gets here if we are using the same marginal
            int myCount = 0;
            for (IntWritable x : values)
                myCount += x.get();
            context.write(key, new DoubleWritable(myCount / (double) marginalCount));
        }
