
    private static final Log LOG = LogFactory.getLog(LogReducer.class);
    private MongoUpdateWritable reduceResult;

    public LogReducer() {
        super();
        reduceResult = new MongoUpdateWritable();
    }

    public void reduce(final Text pKey, final Iterable<IntWritable> pValues, final Context pContext)
        throws IOException, InterruptedException {

        int count = 0;
        for (IntWritable val : pValues) {
            count += val.get();
        }

        BasicBSONObject query = new BasicBSONObject("devices", new ObjectId(pKey.toString()));
        BasicBSONObject update = new BasicBSONObject("$inc", new BasicBSONObject("logs_count", count));
        if (LOG.isDebugEnabled()) {
            LOG.debug("query: " + query);
            LOG.debug("update: " + update);
        }
        reduceResult.setQuery(query);
        reduceResult.setModifiers(update);
        pContext.write(null, reduceResult);
    }

    public void reduce(final Text key, final Iterator<IntWritable> values, final OutputCollector<NullWritable, MongoUpdateWritable> output,
                       final Reporter reporter) throws IOException {
        int count = 0;
        while (values.hasNext()) {
            count += values.next().get();
        }

        BasicBSONObject query = new BasicBSONObject("devices", new ObjectId(key.toString()));
        BasicBSONObject update = new BasicBSONObject("$inc", new BasicBSONObject("logs_count", count));
        if (LOG.isDebugEnabled()) {
            LOG.debug("query: " + query);
            LOG.debug("update: " + update);
        }
        reduceResult.setQuery(query);
        reduceResult.setModifiers(update);
        output.collect(null, reduceResult);
    }

    public void close() throws IOException {
    }

    public void configure(final JobConf job) {
    }
