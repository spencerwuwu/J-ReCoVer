// Standard Deviation

public void reduce(Text key, Iterator<DoubleWritable> values, 
        OutputCollector<Text, DoubleWritable> output, Reporter reporter) throws IOException {
    double stdDev = 0;
    double sumSqr = 0;
    double count = 0;
    double mean = 0;
    double sum = 0;
    while(values.hasNext()){
        double value = values.next().get();
        sumSqr += value * value;
        sum += value;
        count++;
    }
    mean = sum / count;
    stdDev = Math.sqrt((sumSqr - count * mean * mean) / count);
    output.collect(key, new DoubleWritable(stdDev));
}
