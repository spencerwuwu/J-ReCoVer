        public void reduce( Text key, Iterable<LongWritable> values, Context context )
                throws IOException, InterruptedException{
            Long totalUploadFlow = new Long( 0 );
            for ( LongWritable val : values ){
                totalUploadFlow += val.get();
            }
            context.write( key, new LongWritable( totalUploadFlow ) );
        }
