// Idea comes from the TCP back up inverval

public void reduce(Text key, Iterator<IntWritable> values, 
        OutputCollector<Text, Integer> output, Reporter reporter) throws IOException {

    int cur = values.next().get();
    int maxEdge = cur * 2;

    while(values.hasNext()){
        int in = values.next().get();
        if (cur + in > maxEdge) {
            maxEdge = maxEdge * 2;
            cur = 0;
        } else {
            cur += in;
        }
    }

    output.collect(key, cur);
}
