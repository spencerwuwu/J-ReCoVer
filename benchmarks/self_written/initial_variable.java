// The reducer shoudl be commutative because 'index'
// is always larger than 0;
// However, J-ReCoVer would not know the initial value
// during the part of not entering beforeLoop

public void reduce(Text prefix, Iterator<IntWritable> iter,
        OutputCollector<Text, IntWritable> output, Reporter reporter) throws IOException {
    int sumA = 0;
    int sumB = 0;
    int index = 5;

    while(iter.hasNext()) {
        int cur = iter.next().get();
        if (index > 0) sumA += cur;
        else sumB += cur;
        index += 1;
    }

    output.collect(prefix, new IntWritable(sumA > sumB ? sumA : sumB));
}

