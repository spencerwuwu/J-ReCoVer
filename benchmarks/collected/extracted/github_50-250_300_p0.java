		
		private LongWritable result = new LongWritable();
		
		public void reduce(Text key, Iterable<LongWritable> values, Context context) throws IOException, InterruptedException{
			int sum = 0;
			for (LongWritable val : values){
				sum += val.get();
			}
			result.set(sum);
			context.write(key, result);
		}
		
