 	    public void reduce (LongWritable jobId, Iterator<LongWritable> values, OutputCollector<LongWritable, LongWritable> output, Reporter reporter) throws IOException
        {
            while(values.hasNext())
            {
                LongWritable totalJobs = values.next();
                output.collect(jobId, totalJobs);
            }
 	    }
