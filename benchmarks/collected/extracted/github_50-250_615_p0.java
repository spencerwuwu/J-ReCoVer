
        private final IntWritable result = new IntWritable();

        public void reduce( Text key, Iterable<IntWritable> values, Context context )
                throws IOException, InterruptedException{

            int sum = 0;
            for ( final IntWritable val : values ){
                sum += val.get();
            }
            result.set( sum );
            context.write( key, result );
        }
