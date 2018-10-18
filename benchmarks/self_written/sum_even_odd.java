// Sum up the odd and even numbers seperately. Then add the sums.
// Commutable

public void reduce(Text prefix, Iterator<IntWritable> iter,
        OutputCollector<Text, IntWritable> output, Reporter reporter) throws IOException {
    int sumEven = 0;
    int sumOdd = 0;
    int index = 0;

    while(iter.hasNext()) {
        int cur = iter.next().get();
        if (index % 2 == 0) sumEven += cur;
        else sumOdd += cur;
        index += 1;
    }

    output.collect(prefix, new IntWritable(sumEven + sumOdd));
}

