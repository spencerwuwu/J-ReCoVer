

	public void reduce(final Text pKey, final Iterable<IntWritable> pValues, final Context pContext)
			throws IOException, InterruptedException {

		int count = 0;
		for (IntWritable val : pValues) {
			count += val.get();
		}

		pContext.write(pKey, new IntWritable(count));
	}

