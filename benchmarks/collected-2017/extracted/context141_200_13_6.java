

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

