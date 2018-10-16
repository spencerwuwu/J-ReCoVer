		public void reduce(IntWritable k, Iterable<LongWritable> w, Context ctx) {
			long count = 0;
			for (LongWritable lw: w) {
				count = count + lw.get();
			}
			
			// statistics
			ctx.getCounter(FriendCount.COUNT_NODES).increment(1);
			
			WeightedVertex wv = new WeightedVertex();
			wv.id = k.get();
			wv.weight = count;
			
			try {
				ctx.write(NullWritable.get(), wv);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
