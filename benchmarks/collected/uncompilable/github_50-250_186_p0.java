
    private static final Log LOG = LogFactory.getLog(EnronMailReducer.class);
    private BSONWritable reduceResult;
    private IntWritable intw;

    public EnronMailReducer() {
        super();
        reduceResult = new BSONWritable();
        intw = new IntWritable();
    }

    public void reduce(final MailPair pKey, final Iterable<IntWritable> pValues, final Context pContext)
        throws IOException, InterruptedException {
        int sum = 0;
        for (final IntWritable value : pValues) {
            sum += value.get();
        }
        BSONObject outDoc = BasicDBObjectBuilder.start()
          .add("f", pKey.getFrom())
          .add("t", pKey.getTo()).get();
        reduceResult.setDoc(outDoc);
        intw.set(sum);

        pContext.write(reduceResult, intw);
    }

    public void reduce(final MailPair key, final Iterator<IntWritable> values, final OutputCollector<BSONWritable, IntWritable> output,
                       final Reporter reporter)
        throws IOException {
        int sum = 0;
        while (values.hasNext()) {
            sum += values.next().get();
        }
        BSONObject outDoc = BasicDBObjectBuilder.start()
          .add("f", key.getFrom())
          .add("t", key.getTo()).get();
        reduceResult.setDoc(outDoc);
        intw.set(sum);

        output.collect(reduceResult, intw);
    }

    public void close() throws IOException {
    }

    public void configure(final JobConf job) {
    }
