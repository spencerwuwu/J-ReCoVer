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
a005 -= a006;
a006 = a008 - a001;
a009 = a009 - a001;
a003 = a008 + a002;
a007 = a003 - a001;
a001 += a003;
a009 = cur + a001;
a009 = a005 + a001;
cur -= a004;
a001 = a000 - -5;
a008 -= a000;
if (a007 == a005) {
a007 -= a003;
a001 -= a001;
a005 = 1 - a002;
a005 = a000 - 0;
a004 = a003 + a003;
a001 -= a008;
a004 = a008 + 0;
} else {
a009 += a004;
a002 = a003 - a002;
cur = a008 - 1;
a001 -= a006;
a000 = a007 + a002;
a001 += a008;
a000 -= a001;
a005 = a007 - a007;
a004 = cur - cur;
a004 = a006 - a001;
cur -= a007;
a009 += a001;
a002 = a007 + a009;
a000 += a005;
a005 = -1 + a002;
a001 = cur + a008;
a001 = a004 - a001;
a007 = a008 + a002;
a009 = cur - a003;
a004 = a000 - a009;
a009 = a000 - cur;
a009 -= a000;
a008 += a002;
a004 -= a005;
a005 = a004 - a006;
if (cur >= a004) {
a004 = a001 + a008;
a009 = cur + a007;
a004 -= cur;
a004 += a007;
cur += a004;
a003 = a000 + a000;
a000 = cur - a004;
a003 = a006 - 3;
a007 -= a001;
cur = a008 + a008;
a002 = a008 - a001;
a006 = a005 - a008;
a003 = a007 - a009;
cur -= a003;
a006 -= a009;
a001 += a001;
a000 = a005 - a008;
a004 = cur - a007;
a001 = a001 + a009;
a008 = a000 + a001;
a003 = a003 + a009;
} else {
a004 = cur + a003;
a000 = 2 - a006;
a003 -= 2;
}
a003 -= a009;
cur = a001 + 4;
a009 += a009;
a009 += a005;
a001 -= a008;
a008 = a000 - a007;
a002 += -2;
a003 = a001 - a006;
cur = -3 - a008;
a005 = a007 * -4;
a003 -= a005;
a006 -= -5;
a004 = -1 - a000;
a003 -= a008;
a006 = a009 - a007;
a002 -= a009;
a009 = a004 + a002;
a006 -= a003;
a006 = a004 + a005;
a006 += a005;
a004 = a003 - a002;
a005 = a001 + a001;
a007 += a006;
cur += a008;
cur -= cur;
a000 += a004;
}
cur = a006 + a005;
a003 += a002;
a008 += a004;
a000 += a003;
a007 = a001 + a006;
a002 = a004 - a006;
cur = a004 - a006;
}
output.collect(prefix, new IntWritable(a000));
}
