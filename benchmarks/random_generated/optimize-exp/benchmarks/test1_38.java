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
a000 = -4 - a003;
a000 = cur + a000;
if (a001 == a004) {
cur += 0;
a004 += a001;
a002 -= -2;
a004 = a004 + a002;
a004 -= a000;
a001 = a002 + a001;
a003 += a004;
a000 = a000 + a003;
a000 -= cur;
a004 = a002 + a002;
a001 = -2 - cur;
cur -= a001;
a003 = a004 - a001;
cur += a000;
a004 += a003;
a002 = a001 - a004;
a001 -= a000;
a004 = 3 - -4;
cur += cur;
a003 -= a002;
a000 += a000;
a001 += 2;
a004 -= a001;
a002 -= a002;
a002 = a001 + a003;
a000 -= a003;
a001 = a001 + a004;
cur = a004 + a002;
a003 = a003 - a003;
a004 = a000 + a001;
a003 = 2 - a002;
a003 -= a004;
a004 = a003 - a001;
} else {
a002 += 0;
a001 = a002 * 3;
a002 -= a000;
a004 = a004 + a004;
a000 += cur;
cur = -1 - cur;
a003 = a000 - a003;
cur = a004 + a001;
a000 = cur - a001;
a002 -= cur;
a004 = a002 - a004;
a001 += a000;
}
a004 = a001 - cur;
a003 += cur;
a002 = a000 + a001;
}
output.collect(prefix, new IntWritable(a004));
}
