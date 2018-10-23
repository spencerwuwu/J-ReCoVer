

	  private boolean useCounts = true;
	  
	  public void reduce(Text key, Iterable<LongWritable> values, Context context)
			    throws IOException, InterruptedException {
			    if (useCounts) {
			      long sum = 0;
			      for (LongWritable value : values) {
			        sum++;
			      }
			      context.write(new Text(key.toString() + ',' + sum), null);
			    } else {
			      context.write(new Text(key.toString()), null);
			    }
			  }

