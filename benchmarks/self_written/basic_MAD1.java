// basic - Mean Absolute Deviation part1
// Calculate the average
// Cont. in part 2

public void reduce(Text key, Iterator<IntWritable> iter,
        OutputCollector<Text, DoubleWritable> output, Reporter reporter) throws IOException {
    int cnt = 0;
    double sum = iter.next().get();

    while(iter.hasNext()) {
        int cur = iter.next().get();
        sum += cur;
        cnt += 1;
    }

    output.collect(key, new DoubleWritable(sum / cnt));
}

