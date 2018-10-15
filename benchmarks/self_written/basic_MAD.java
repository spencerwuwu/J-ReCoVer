// basic - Mean Absolute Deviation
// Not a real MAD, use random value as avg

public void reduce(Text key, Iterator<IntWritable> iter,
        OutputCollector<Text, DoubleWritable> output, Reporter reporter) throws IOException {
    int cnt = 0;
    int mad = 0;
    double avg = (double)(Math.random() * 1000 + 1) - 500;

    while(iter.hasNext()) {
        int cur = iter.next().get();

        if (cur < avg) mad += ret - cur;
        else mad += cur - avg;
    }

    output.collect(key, new DoubleWritable(mad / cnt));
}

