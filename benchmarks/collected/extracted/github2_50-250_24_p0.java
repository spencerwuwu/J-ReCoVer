
		/**
		 * Reduce phase: sum up the number of edges of the triangle.
		 * If there are 3 edges, then it is a complete triangle and 
		 * we output it.
		 */
		public void reduce(Text key, Iterable<LongWritable> values,
				Context context) throws IOException, InterruptedException {
			Iterator<LongWritable> iter = values.iterator();
			int sum = 0;
			while(iter.hasNext()) {
				sum += iter.next().get();
			}
			if(sum == 3) { // Output if it has 3 edges
				context.write(key, null);
			}
		}

