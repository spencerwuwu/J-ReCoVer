// basic - avg

public void reduce(Text key, Iterator<IntWritable> iter,
        OutputCollector<Text, DoubleWritable> output, Reporter reporter) throws IOException {
    int ret = 0;
    int cnt = 0;

    while(iter.hasNext()) {
        int cur = iter.next().get();
        ret += cur;
        cnt += 1;
    }

    output.collect(key, new DoubleWritable(ret / cnt));
}

