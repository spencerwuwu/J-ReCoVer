// Inaccuracy of floating points


private final double D = 0.85;
private DoubleWritable result = new DoubleWritable();

public void reduce(Text key, Iterable<DoubleWritable> values, Context context)
		throws IOException, InterruptedException {

	double sum = 1.0 - D;
	for (DoubleWritable value : values) {
		sum += value.get();
	}
	result.set(sum);

	context.write(result, key);
}
