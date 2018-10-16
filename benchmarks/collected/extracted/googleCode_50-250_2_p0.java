		public void reduce(Text k, Iterable<LongWritable> vals, Context ctx) {
			long count = 0;
			for (LongWritable lw: vals) {
				count = count + lw.get();
			}
		
			try {
				ctx.write(k, new LongWritable(count));
			} catch (Exception e) {
				e.printStackTrace();
			} 
		}
