
		NullWritable nullKey =NullWritable.get();
		
		public void reduce(Text key, Iterable<IntWritable> values,Context context)
				throws IOException, InterruptedException {
			int sum = 0;
			Iterator<IntWritable> it = values.iterator();
			while(it.hasNext()){
				sum += it.next().get();
			}
			context.write(nullKey, new Text(key.toString().replace(":", ",") + "," + sum));
		}
		
