		public void reduce(Edge k, Iterable<LongWritable> vals, Context ctx) {
			long count = 0;
			for (LongWritable lw: vals) {
				count = count + lw.get();
			}
			
			Counter ct = ctx.getCounter(MyGroup.WEGGEFALLEN_COUNT);
			
			int n = ctx.getConfiguration().getInt("n", 5);
			if (count >= n-2) {
				try {
					ctx.write(NullWritable.get(), k);
				} catch (Exception e) {
					e.printStackTrace();
				} 
			} else {
				ct.increment(1);
			}
		}
