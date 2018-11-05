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
a006 += a003;
cur += a002;
a000 = a008 + cur;
a006 += a002;
a008 += a007;
a003 = a000 + a001;
a001 -= a003;
a009 -= cur;
a005 -= a004;
a008 += a002;
a006 = a009 - a003;
cur += a006;
cur += a007;
a001 += a005;
a000 += a002;
a009 = a001 + a008;
a005 = a007 + a006;
a008 -= a001;
a000 = a009 - a005;
a005 -= a002;
a008 = a005 + a007;
a002 = a002 - a000;
a008 = a005 + -1;
a001 = a004 + a000;
if (a001 == 4) {
a009 += -1;
a006 -= a005;
a001 -= a004;
a009 = a000 + a005;
a007 = a000 + a002;
a004 -= a002;
a004 = a009 - -5;
a003 += a009;
a007 += a001;
a003 = a003 - a004;
a001 = a001 - a006;
a002 = 1 + a007;
if (a009 != a006) {
a001 = a009 - a002;
a006 -= a004;
a007 = a008 - 4;
a009 = a004 + a001;
a009 -= a003;
a003 = cur - a000;
a007 -= a000;
a001 += a000;
a000 = -1 + a005;
a005 = a008 + a001;
a000 += a007;
a007 += a004;
a009 += a008;
a000 -= a006;
a004 = a003 + a006;
a000 = a003 - a005;
a009 = a005 - a003;
a006 = a009 - 2;
a001 -= a000;
a004 += a009;
a003 = a001 + a008;
a009 = a003 - a000;
a002 += cur;
a006 = a004 - a005;
a003 = a009 + a006;
a008 += a003;
a009 = a005 + a000;
a004 = a008 + a001;
a005 += a009;
a004 -= a002;
a002 -= a008;
} else {
a008 = a008 - a007;
a008 = a000 - a008;
a009 += a005;
a003 = a005 - a002;
a008 = a003 + a006;
cur = 0 + a007;
a001 = a005 + a007;
a006 = a008 + a007;
a004 -= a006;
a003 -= a007;
a008 = a007 - a009;
a007 = a008 - a006;
a009 = a008 - cur;
cur = a009 - a008;
a001 -= a009;
a009 = a006 + a007;
a002 = a004 - a007;
a003 = a002 + cur;
a001 += -2;
}
a007 -= a008;
a004 = -4 + -2;
a004 = a000 - a005;
a001 += a004;
a009 = a001 - a000;
a004 = a001 + a004;
} else {
}
cur += a005;
a001 += a001;
a002 = a009 - a004;
a002 = a004 + a006;
cur += a003;
a001 = a001 + cur;
cur -= cur;
a000 = a009 + a009;
}
output.collect(prefix, new IntWritable(a009));
}
