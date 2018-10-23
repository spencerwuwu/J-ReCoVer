
    private static final Log LOG = LogFactory.getLog(TreasuryYieldReducer.class);
    private BSONWritable reduceResult;

    public TreasuryYieldReducer() {
        super();
        reduceResult = new BSONWritable();
    }

    public void reduce(final IntWritable pKey, final Iterable<DoubleWritable> pValues, final Context pContext)
        throws IOException, InterruptedException {

        int count = 0;
        double sum = 0;
        for (final DoubleWritable value : pValues) {
            sum += value.get();
            count++;
        }

        final double avg = sum / count;

        if (LOG.isDebugEnabled()) {
            LOG.debug("Average 10 Year Treasury for " + pKey.get() + " was " + avg);
        }

        BasicBSONObject output = new BasicBSONObject();
        output.put("count", count);
        output.put("avg", avg);
        output.put("sum", sum);
        reduceResult.setDoc(output);
        pContext.write(pKey, reduceResult);
    }

    public void reduce(final IntWritable key, final Iterator<DoubleWritable> values,
                       final OutputCollector<IntWritable, BSONWritable> output,
                       final Reporter reporter) throws IOException {
        int count = 0;
        double sum = 0;
        while (values.hasNext()) {
            sum += values.next().get();
            count++;
        }

        final double avg = sum / count;

        if (LOG.isDebugEnabled()) {
            LOG.debug("Average 10 Year Treasury for " + key.get() + " was " + avg);
        }

        BasicBSONObject bsonObject = new BasicBSONObject();
        bsonObject.put("count", count);
        bsonObject.put("avg", avg);
        bsonObject.put("sum", sum);
        reduceResult.setDoc(bsonObject);
        output.collect(key, reduceResult);
    }

    public void close() throws IOException {
    }

    public void configure(final JobConf job) {
    }
