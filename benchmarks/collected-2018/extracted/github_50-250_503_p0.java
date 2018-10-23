        public void reduce( Text key, Iterable<LongWritable> values, Context context )
                throws IOException, InterruptedException{
            ArrayList<Long> outputFlowArray = new ArrayList<Long>();
            for ( LongWritable val : values ){
                outputFlowArray.add( val.get() );
            }
            Long totalOutput = Collections.max( outputFlowArray ) - Collections.min( outputFlowArray );
            context.write( key, new LongWritable( totalOutput ) );
        }
