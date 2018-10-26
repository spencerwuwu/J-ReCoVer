// basic - rangeSum

public void reduce(Text key, Iterator<IntWritable> iter,
        OutputCollector<Text, IntWritable> output, Reporter reporter) throws IOException {
    int ret = 0;
    int cnt = 0;
    int i = 0;
    int N = J_RECOVER_ITER_NUM.get();

    while(iter.hasNext()) {
        int cur = iter.next().get();
        if (i > N / 2) {
            ret += cur;
            cnt += 1;
        }
        i++;
    }

    if (cnt != 0)
        output.collect(key, new IntWritable(ret));
    else 
        output.collect(key, new IntWritable(0));
            
}
