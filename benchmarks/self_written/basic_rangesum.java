// basic - rangesum

public void reduce(Text key, Iterator<IntWritable> iter,
        OutputCollector<Text, IntWritable> output, Reporter reporter) throws IOException {
    int ret = 0;
    int cnt = 0;

    while(iter.hasNext()) {
        int cur = iter.next().get();

        if (cnt > 4) {
            ret += cur;
        }
        cnt += 1;
    }

    if (cnt != 0)
        output.collect(key, new DoubleWritable(ret / cnt));
    else
        output.collect(key, new DoubleWritable(0));
}
