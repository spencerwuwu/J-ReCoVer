// Nested loop
// Loop is considered as an if-condition

private long total;
private long kept = 0;
private float keep;

public void reduce(LongWritable key, Iterator<LongWritable> values,
		OutputCollector<LongWritable,LongWritable> output, Reporter reporter)
				throws IOException {
	while (values.hasNext()) {
        ++total;
        while((float) kept / total < keep) {
            ++kept;
            output.collect(key, values.next());
        }
	}
}
