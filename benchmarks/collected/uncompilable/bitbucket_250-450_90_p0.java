
NullWritable n = NullWritable.get();
public void reduce(Text key, Iterator<LongWritable> values,
        OutputCollector<PageviewRecord, NullWritable> output, Reporter reporter)
    throws IOException {

    long sum = 0L;
    while(values.hasNext()) {
        sum += values.next().get();
    }
    output.collect(new PageviewRecord(key.toString(), sum), n);
}
