// basic - dis

public void reduce(Text key, Iterator<IntWritable> iter,
        OutputCollector<Text, DoubleWritable> output, Reporter reporter) throws IOException {
    int ret = 0;
    int cnt = 0;

    while(iter.hasNext()) {
        int cur = iter.next().get();

        if (cur > 100) {
            ret += cur;
            cnt += 1;
        }
    }

    if (cnt != 0)
        output.collect(key, new DoubleWritable(ret / cnt));
    else
        output.collect(key, new DoubleWritable(0));
}
