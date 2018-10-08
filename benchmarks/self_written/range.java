// Find the range of the values

public void reduce(Text prefix, Iterator<IntWritable> iter,
        OutputCollector<Text, IntWritable> output, Reporter reporter) throws IOException {
    int cur = iter.next().get();
    int max = cur;
    int min = cur;
    while(iter.hasNext()) {
        cur = iter.next().get();
        max = cur > max ? cur : max;
        min = cur < min ? cur : min;
    }
    output.collect(prefix, new IntWritable(max - min));
}
