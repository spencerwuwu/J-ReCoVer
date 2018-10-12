// Note: only +, - operations
// Parameters:
//   Variables:   2
//   Baselines:   10
//   If-Branches: 1

public void reduce(Text prefix, Iterator<IntWritable> iter,
		OutputCollector<Text, DoubleWritable> output, Reporter reporter) throws IOException {
	double a = 0;
	double b = 0;
	double cur = 0;

	while (iter.hasNext()) {
		cur = iter.next().get();
		cur = a - -2;
		if (cur >= a) {
			b -= a;
			a = b - cur;
			cur -= a;
			b = a + a;
		} else {
			a = cur - a;
			b -= cur;
			cur = b + cur;
			b = 3 + b;
			b = a + 0;
			b = -4 + a;
		}
		output.collect(prefix, new DoubleWritable(cur));
	}
