// A program with complicate condition branches
// Just making it hard to determine commutativity in one sight
//
// Counter Example:
//  Input1: [3, 2, 3, 1]
//  Input2: [1, 3, 2, 3]
//  Output1: [20]
//  Output2: [6]

public void reduce(Text prefix, Iterator<IntWritable> iter,
        OutputCollector<Text, IntWritable> output, Reporter reporter) throws IOException {
    int cur = iter.next().get();
    int max = cur;
    int min = cur;
    int sum1 = 0;
    int sum2 = 0;
    while(iter.hasNext()) {
        cur = iter.next().get();
        max = cur > max ? cur : max;
        min = cur < min ? cur : min;
        if (max > 10) {
            if (sum1 == 3) {
                sum1 -= cur;
                max += 1;
                sum2 = min + max;
            } else {
                if (sum1 + min == 3) {
                    sum2 += cur;
                    max *= 3;
                } else {
                    min = 0;
                    if (sum2++ > sum1) {
                        sum1 = sum2 + max + min;
                    }
                }
                min -= max;
                sum2 -= 1;
            }
        } else {
            if (sum1 == 3) {
                sum1 -= cur;
                sum2 += cur;
            } else {
                if (sum1 + min == 3) {
                    max *= 3;
                    sum2 = min + max;
                    max += 1;
                } else {
                    min = 0;
                    if (sum2++ > sum1) {
                        sum1 = sum2 + max + min;
                    }
                }
                min += max;
                sum2 /= 2;
            }
        }
    }
    output.collect(prefix, new IntWritable(sum1 + sum2));
}

