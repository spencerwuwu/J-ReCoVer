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
if (a003 > a002) {
a002 += a007;
a003 += a002;
a007 -= a009;
a008 = -4 - a007;
a005 = a003 - a005;
a004 = a009 - a004;
a006 += a006;
a006 = a001 - a002;
a006 = a004 - a003;
a003 += a003;
a003 -= a008;
a005 -= a005;
a004 += a000;
a006 = a001 - a000;
a002 = a001 - a003;
a002 = a009 + a003;
a009 += cur;
a001 = -4 - a001;
a008 = a008 + a007;
a008 = a001 - a000;
a005 = a006 - a002;
a009 -= a001;
a004 = a008 + a000;
a000 = a007 + -1;
a002 = a004 + cur;
if (a005 != a009) {
a003 = a001 - a007;
} else {
a004 = -5 - a004;
a007 -= a009;
a004 -= a001;
a000 = a004 + a006;
a002 += a003;
a005 = a004 + a007;
a007 += a000;
a002 -= -1;
a007 -= a009;
a008 = a003 + a003;
a001 -= a004;
a001 += a008;
cur = a004 - a008;
a003 += cur;
a001 += 4;
a006 += a004;
cur = a006 + a000;
a004 -= -1;
a008 -= a006;
a009 += a006;
a002 = a009 - a000;
a008 -= a002;
a007 = a009 - a001;
a007 -= -2;
}
a007 = 2 + a006;
a006 = a002 - cur;
a006 -= a000;
cur = a009 + cur;
a008 = a007 - a008;
a002 += a006;
a003 -= a007;
a008 = cur - a005;
cur -= a007;
a002 += a007;
cur -= -2;
a001 = a005 + cur;
a009 = a001 + a000;
a005 -= a002;
a001 = a007 + a009;
a007 = a006 - -2;
a000 = a004 - a009;
a005 -= a005;
a004 = a009 + a001;
a009 = a005 + a009;
a005 = a007 - 2;
cur = a005 + a001;
a008 -= a000;
a006 = a007 - a000;
a004 += cur;
a001 -= a001;
a007 += a003;
a001 += a006;
cur = a006 + a002;
a007 = a009 + cur;
} else {
a001 -= a002;
a003 = a001 + 4;
a000 = a000 + a002;
a007 += a002;
}
a005 = 2 + 3;
a002 += a008;
a008 -= a007;
a003 = a007 + a007;
a002 -= a002;
a007 = a001 - a003;
a002 = a007 - a000;
a004 -= a007;
a009 = a008 - a003;
a000 += a009;
a006 = a004 + a007;
a008 = cur - -5;
a002 = 1 - a009;
a001 = a002 - a000;
a006 += a005;
a008 = a004 + a002;
}
output.collect(prefix, new IntWritable(cur));
}
