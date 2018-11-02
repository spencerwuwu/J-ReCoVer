// Note: only +, - operations
// Parameters:
//   Variables:   10
//   Baselines:   100
//   If-Branches: 2

public void reduce(Text prefix, Iterator<IntWritable> iter,
         OutputCollector<Text, IntWritable> output, Reporter reporter) throws IOException {
int a000 = 0;
int a001 = 0;
int a002 = 0;
int a003 = 0;
int a004 = 0;
int a005 = 0;
int a006 = 0;
int a007 = 0;
int a008 = 0;
int a009 = 0;
int cur = 0;

while (iter.hasNext()) {
cur = iter.next().get();
a003 += -1;
if (a001 > a002) {
a007 += a008;
cur -= a007;
a006 = a008 + a002;
a001 += a004;
a008 = a007 + a004;
a004 = -2 + a007;
a004 = a001 - a003;
a000 -= a004;
a009 -= a009;
a009 -= a002;
a001 += a008;
a009 -= a003;
a008 = a006 + a006;
a008 = a004 + a009;
a005 -= cur;
cur = a003 + a001;
cur = a008 + a009;
cur += a000;
a005 = a008 + a008;
a000 += a007;
a002 -= a003;
a006 += -3;
a000 += a007;
a002 -= a001;
a009 -= a001;
a008 += a004;
} else {
a007 = a006 + a005;
a002 = a002 + 2;
a000 = a003 - a003;
a001 += a002;
a009 -= cur;
a003 = a000 * -5;
if (a004 != a002) {
a009 -= a009;
a006 += a002;
a006 -= a002;
cur = a006 - a009;
a002 -= a006;
a009 = a004 + a005;
a007 = a008 + a004;
a003 -= a009;
a001 -= a009;
a007 = a001 + a007;
cur += a002;
a007 = a007 - a009;
a002 = a009 - cur;
a000 -= a000;
a003 -= cur;
a009 = a004 - a000;
a003 = a009 - a001;
a003 -= a001;
a002 = -3 - a004;
a003 -= a008;
a003 = a007 + a006;
a004 = a001 - a006;
a003 = a005 + a007;
a007 -= a008;
a001 += a003;
a008 -= cur;
cur = a003 - a006;
a001 += a005;
a006 += a005;
a005 -= a002;
} else {
}
a008 -= a004;
a005 += 0;
a004 += a000;
a001 = a005 + a000;
a001 -= a001;
cur = a000 + a008;
a004 -= a000;
a003 -= a001;
a000 -= a002;
a006 -= cur;
a009 -= a002;
a007 = a008 - cur;
a002 = cur - a003;
}
a004 = a005 * -1;
a002 += a009;
a001 -= a008;
a000 = -4 - a003;
a009 = a006 - a003;
a000 = a004 - a005;
a001 = a004 + a003;
cur = a007 - 1;
a003 = a001 - a000;
a004 = a005 + a005;
a001 += a009;
a003 = a006 + a008;
a006 -= a002;
a008 = a006 - a003;
a004 = a009 - a002;
cur += a003;
a000 = cur - a001;
a002 += a007;
a001 = cur - 3;
a007 -= a003;
a006 -= a004;
a006 = a009 - a007;
a007 -= a001;
a003 -= -3;
}
output.collect(prefix, new IntWritable(a004));
}
