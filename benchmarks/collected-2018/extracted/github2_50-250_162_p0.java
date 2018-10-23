		
		/*
		 * This method should calculate the maximum row length for each file 
		 */
		public void reduce(Text fileName, Iterable<IntWritable> arg1, Context context) throws IOException ,InterruptedException {
			
			
			int maxValue = Integer.MIN_VALUE;
			
			//TODO: calculate the maximum row length per fileName

			context.write(fileName, new IntWritable(maxValue));
		};
	
