		
		public IntWritable index=new IntWritable(1);
		
		public void reduce(IntWritable key,Iterable<IntWritable> values,Context context) throws IOException, InterruptedException{
			
			context.write(index, key);
			index=new IntWritable(index.get()+1);
			
		}
