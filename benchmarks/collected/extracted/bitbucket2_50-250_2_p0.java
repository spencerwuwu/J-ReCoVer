	private long n = 0;
	
	/** Reducer setup */
	/*
	public void setup (Context context) {
		//Get the value of n from the job configuration
		n = context.getConfiguration().getLong("n", 0);
	}
	*/

	
    public void reduce(LongWritable key, Iterable<LongWritable> values, Context context) throws IOException, InterruptedException {
    	LongWritable new_key = new LongWritable(0);
    	LongWritable new_value = new LongWritable(0);
    	
    	//TODO: Define the REDUCE function
    	
    	for(LongWritable value : values){
    		
		//define content of reduce

        	context.write(new_key, new_value);

    	}
    }
