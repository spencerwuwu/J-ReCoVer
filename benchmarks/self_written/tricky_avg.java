// 

public void reduce(Text key, Iterator<IntWritable> values, 
        OutputCollector<Text, DoubleWritable> output, Reporter reporter) throws IOException {
    double avg = 0;
    int sum1 = 0;
    int count1 = 0;
    int sum2 = 0;
    int count2 = 0;
    int sum3 = 0;
    int count3 = 0;

    while(values.hasNext()){
        int cur = values.next().get();
        if (cur < 10) {
            sum1 += cur;
            count1 += 1;
        } else if (cur >= 20) {
            sum3 += cur;
            count3 += 1;
        } else {
            sum2 += cur;
            count2 += 1;
        }
    }
    avg = (sum1 + sum2 + sum3) / (count1 + count2 + count3);

    output.collect(key, new DoubleWritable(avg));
}
