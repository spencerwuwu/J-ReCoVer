    public void reduce(Text location,
                       Iterable<IntWritable> sightings,
                       Context pContext )
            throws IOException, InterruptedException{
        int count = 0;
        for ( final IntWritable v: sightings){
            //LOG.debug( "Location: " + location + " Value: " + v );
            count += v.get();
        }

        pContext.write( location, new IntWritable( count ) );
    }

    //private final Log LOG = LogFactory.getLog( UfoSightingsReducer.class );

