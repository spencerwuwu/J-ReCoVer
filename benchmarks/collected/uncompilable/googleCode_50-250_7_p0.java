		public void reduce(IntWritable k, Iterable<IntWritable> vals, Context ctx) {
			ArrayList<Integer> al = new ArrayList<Integer>();
			for (IntWritable iw: vals) {
				int cur = iw.get();
				al.add(cur);
			}

			
			for (int i = 0; i < al.size()-1; i++) {
				for (int j = i+1; j < al.size(); j++) {
					Triangle t = new Triangle(k.get(), al.get(i), al.get(j));
					ctx.getCounter(TriangleCountS4.COUNT_TRIANGLES).increment(1);
					try {
						ctx.write(NullWritable.get(), t);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}
