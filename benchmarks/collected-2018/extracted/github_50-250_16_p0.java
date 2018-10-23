		
		public void reduce(Text key,Iterable<IntWritable> values,Context context) throws IOException, InterruptedException{
			int sum=0;
			int count=0;
			
			for(IntWritable i : values){
				sum+=i.get();
				count++;
			}
			context.write(key, new IntWritable(sum/count));
			
		}
