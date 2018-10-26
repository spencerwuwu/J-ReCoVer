// basic - max

public void reduce(Text key, Iterator<IntWritable> iter,
        OutputCollector<Text, IntWritable> output, Reporter reporter) throws IOException {
    int ret = iter.next().get();

    while(iter.hasNext()) {
        int cur = iter.next().get();
        ret = ret < cur ? cur : ret;
    }

    output.collect(key, new IntWritable(ret));
}
