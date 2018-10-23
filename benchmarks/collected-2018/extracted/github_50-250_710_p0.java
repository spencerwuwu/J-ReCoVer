    public void reduce(Text pCountryCode,
                       Iterable<DoubleWritable> pValues,
                       Context pContext )
            throws IOException, InterruptedException{
        double count = 0;
        double sum = 0;
        for ( final DoubleWritable value : pValues ){
            sum += value.get();
            count++;
        }

        final double avg = sum / count;

        pContext.write( pCountryCode, new DoubleWritable( avg ) );
    }

    //private final Log LOG = LogFactory.getLog( WorldDevIndicatorReducer.class );
