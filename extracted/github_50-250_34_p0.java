        public void reduce( Text key, Iterator<IntWritable> values,
                            OutputCollector<Text, IntWritable> output,
                            Reporter reporter) throws IOException
        {
            // Iterate over all of the values (counts of occurrences of this word)
            int count = 0;
            while( values.hasNext() )
            {
                // Add the value to our count
                count += values.next().get();
            }

            // Output the word with its count (wrapped in an IntWritable)
            output.collect( key, new IntWritable( count ) );
        }
