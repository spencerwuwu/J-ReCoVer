// basic - sum

public void reduce(Text key, Iterator<IntWritable> iter,
        OutputCollector<Text, IntWritable> output, Reporter reporter) throws IOException {
    int ret = 0;

    while(iter.hasNext()) {
        int cur = iter.next().get();
        ret += cur;
    }

    output.collect(key, new IntWritable(ret));
}
