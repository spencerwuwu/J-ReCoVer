
    //private static final Log LOG = LogFactory.getLog(LogCombiner.class);

    public void reduce(final Text pKey, final Iterable<IntWritable> pValues, final Context pContext)
        throws IOException, InterruptedException {

        int count = 0;
        for (IntWritable val : pValues) {
            count += val.get();
        }

        pContext.write(pKey, new IntWritable(count));
    }


    public void close() throws IOException {
    }

    public void configure(final JobConf job) {
    }
