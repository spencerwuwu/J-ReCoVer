

	int j = 0;

	private int k = 0;
	private final int KEY_NUMBER = 500000;

	public void reduce(Text key, Iterable<IntWritable> values, Context context) throws IOException, InterruptedException {
		int delay = 1;
		int sum = 0;
		for (IntWritable i: values){
			sum += i.get();
			if (k * KEY_NUMBER < j){
				Thread.sleep(delay * 1000);
				k++;
			}
			j+=i.get();

		}
		//	            Thread.sleep(3 + sum / delay);
		context.write(key, new IntWritable(sum));
	}

