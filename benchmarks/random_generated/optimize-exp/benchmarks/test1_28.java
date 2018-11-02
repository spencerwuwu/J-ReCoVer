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
cur -= a004;
cur += -5;
a001 -= -3;
cur -= a002;
if (a003 <= a003) {
a003 -= a003;
a004 = cur - a003;
a000 = a002 - a001;
a000 += a001;
a003 = a001 + a004;
cur += a004;
a001 -= a000;
a004 -= -4;
} else {
a002 += a000;
cur += cur;
cur -= a000;
a001 = a004 + a000;
a002 = a004 + a001;
a002 = a001 - a001;
a000 += a002;
cur = a004 + cur;
a000 = a002 - a000;
a000 += cur;
cur = 4 - a001;
a001 += a001;
a004 -= a000;
a004 -= a003;
a000 = a004 + a000;
a003 += cur;
}
a002 -= a000;
a000 -= a000;
a004 += a000;
cur += a002;
a003 -= a000;
a003 = a000 - a002;
a002 += -3;
a003 += a004;
cur -= a000;
a001 = cur + -4;
a001 = a001 - a003;
a003 -= cur;
a000 -= a003;
cur = 2 + a002;
a004 = -2 - a002;
cur += 1;
a000 = a003 - cur;
a001 = a004 + a001;
a003 = a002 * -1;
cur -= 3;
a002 += a003;
a000 += a002;
}
output.collect(prefix, new IntWritable(a003));
}
