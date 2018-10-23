

	private DoubleWritable outvalue = new DoubleWritable();


	public void reduce(Text key, Iterable<DoubleWritable> values,
			Context context) throws IOException, InterruptedException {

		double sum = 0.0;
		double count = 0;
		for (DoubleWritable dw : values) {
			sum += dw.get();
			++count;
		}

		outvalue.set(sum / count);
		context.write(key, outvalue);
	}

