
    private static final Log LOG = LogFactory.getLog(TreasuryYieldReducer.class);
    private MongoUpdateWritable reduceResult;

    public TreasuryYieldUpdateReducer() {
        super();
        reduceResult = new MongoUpdateWritable();
    }

    public void reduce(final IntWritable pKey,
                       final Iterable<DoubleWritable> pValues,
                       final Context pContext)
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

        BasicBSONObject query = new BasicBSONObject("_id", pKey.get());
        BasicBSONObject modifiers = new BasicBSONObject();
        modifiers.put("$set", BasicDBObjectBuilder.start()
                                                  .add("count", count)
                                                  .add("avg", avg)
                                                  .add("sum", sum)
                                                  .get());

        modifiers.put("$push", new BasicBSONObject("calculatedAt", new Date()));
        modifiers.put("$inc", new BasicBSONObject("numCalculations", 1));
        reduceResult.setQuery(query);
        reduceResult.setModifiers(modifiers);
        pContext.write(null, reduceResult);
    }

