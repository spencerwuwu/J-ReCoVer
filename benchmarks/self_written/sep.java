// basic - sep

public void reduce(Text key, Iterator<IntWritable> iter,
        OutputCollector<Text, IntWritable> output, Reporter reporter) throws IOException {
    int ret = 0;

    while(iter.hasNext()) {
        int cur = iter.next().get();

        if (cur % 2 == 0) ret += 1;
        else ret -= 1;
    }

    output.collect(key, new IntWritable(ret));
}

