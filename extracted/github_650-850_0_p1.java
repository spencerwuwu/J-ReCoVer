public void configure(JobConf job) {
}

public void reduce(IntWritable key, Iterator<IntWritable> it,
        OutputCollector<IntWritable, IntWritable> out,
        Reporter reporter) throws IOException {
    int keyint = key.get();
    int count = 0;
    while (it.hasNext()) {
        it.next();
        count++;
    }
    out.collect(new IntWritable(keyint), new IntWritable(count));
}
public void close() {
}
