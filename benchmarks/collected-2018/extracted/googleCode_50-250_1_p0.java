		public void reduce(IntWritable k, Iterable<LongWritable> vals, Context ctx) {
			long count = 0;
			for (LongWritable lw: vals) {
				count = count + lw.get();
			}
		
			try {
				ctx.write(new IntWritable(k.get()), new LongWritable(count));
			} catch (Exception e) {
				e.printStackTrace();
			} 
		}
