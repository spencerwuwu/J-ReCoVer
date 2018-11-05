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
a001 -= 4;
a001 -= cur;
cur -= a001;
a004 = a000 + a001;
a004 += -5;
a002 = a003 - a003;
a000 -= cur;
a004 = a002 + a002;
a002 = a003 - a001;
a004 += 3;
a002 = a003 + 3;
a003 -= -2;
a004 -= a000;
a001 = 4 - cur;
cur = -2 + a001;
a001 = a001 - a000;
a004 -= a002;
a002 -= a002;
a000 = a000 + a002;
a002 += a002;
a000 -= cur;
a001 = a000 + a004;
a004 = -1 - 3;
a002 = a003 - a000;
a001 -= a002;
a000 = a004 + a004;
if (a001 == 0) {
a004 += cur;
a000 -= a002;
a001 -= cur;
a004 -= -4;
a004 = a004 + a003;
a002 += cur;
cur = cur - a001;
a004 = 4 - cur;
a004 += a002;
a001 = -4 + a004;
a003 -= a004;
a001 = a000 + a001;
a002 -= a002;
a003 += 1;
a002 = -1 - a000;
a004 = a004 - a002;
a003 += cur;
a003 += a002;
a003 = 4 + a002;
cur = -1 - a003;
a000 = a004 - cur;
a000 = -1 + a004;
a004 = -2 + a002;
a001 += a000;
} else {
}
}
output.collect(prefix, new IntWritable(a002));
}
