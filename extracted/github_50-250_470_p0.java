    /** Flag is to check that mapper was configured before run. */
    private boolean wasConfigured;

    /** {@inheritDoc} */
    @Override public void reduce(Text key, Iterator<IntWritable> values, OutputCollector<Text, IntWritable> output, Reporter reporter)
            throws IOException {
        assert wasConfigured : "Reducer should be configured";

        int sum = 0;

        while (values.hasNext())
            sum += values.next().get();

        output.collect(key, new IntWritable(sum));
    }

    @Override public void configure(JobConf job) {
        super.configure(job);

        wasConfigured = true;
    }
