// basic - Mean Absolute Deviation part2
// Avarge calculated in part1, use random value here

public void reduce(Text key, Iterator<IntWritable> iter,
        OutputCollector<Text, DoubleWritable> output, Reporter reporter) throws IOException {
    int cnt = 0;
    int mad = 0;
    double avg = 50;

    while(iter.hasNext()) {
        int cur = iter.next().get();

        if (cur < avg) mad += avg - cur;
        else mad += cur - avg;
	cnt += 1;
    }

    output.collect(key, new DoubleWritable(mad / cnt));
}

