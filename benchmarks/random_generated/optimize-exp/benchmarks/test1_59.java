// Note: only +, - operations
// Parameters:
//   Variables:   5
//   Baselines:   50
//   If-Branches: 1

public void reduce(Text prefix, Iterator<IntWritable> iter,
         OutputCollector<Text, IntWritable> output, Reporter reporter) throws IOException {
int a000 = 0;
int a001 = 0;
int a002 = 0;
int a003 = 0;
int a004 = 0;
int cur = 0;

while (iter.hasNext()) {
cur = iter.next().get();
a003 -= a002;
a003 = a002 - 1;
a002 -= a001;
a003 = a002 - 2;
cur = a001 - 0;
a001 = a003 - a004;
cur += a002;
a001 = cur - a004;
a004 -= cur;
a001 = a002 + a000;
a000 = -4 + a000;
a002 = a004 + a000;
a004 += a001;
cur = 3 + cur;
a002 += a000;
cur = a000 - a003;
a002 = a003 + a001;
a003 -= a000;
a004 = a004 - a002;
a000 = a004 + cur;
a001 += a000;
a004 = a004 + a004;
a003 += cur;
a001 -= a001;
a001 = a003 - a004;
a003 = a000 - a001;
a000 -= a004;
a000 = a003 + -2;
a000 = a002 - cur;
a002 -= -4;
a003 = a001 + a003;
a001 += a000;
if (a000 < a003) {
cur = a001 + a003;
a000 -= cur;
cur -= a003;
a002 -= a001;
a001 = a001 * -3;
cur -= cur;
a003 = a004 + 1;
a001 -= -5;
a002 -= a000;
a003 = a000 - a001;
a001 = a002 + a004;
a002 -= cur;
} else {
a000 = a003 + a000;
a004 = a000 - a004;
a004 -= cur;
a003 += cur;
}
a003 = a004 + 0;
a003 = cur - a002;
}
output.collect(prefix, new IntWritable(a004));
}
