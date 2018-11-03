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
a005 -= a003;
a006 = a004 - a002;
cur += a007;
a006 = a002 + a008;
a009 += -2;
a008 += a004;
cur = a008 - a002;
a002 = cur - a006;
a007 = a005 + a008;
a001 -= a006;
a006 -= a009;
a002 = a003 - -5;
a005 = a001 + a000;
a000 = a000 - a001;
a006 = a001 + cur;
a008 = a001 + a009;
if (a005 <= cur) {
a003 += 0;
a005 -= a002;
a003 = a006 + a000;
a001 = a004 + a007;
a007 = a000 + a009;
a004 -= 4;
a002 = a005 - a008;
a008 = cur - a005;
a007 = a000 - a002;
a001 = a009 - a000;
a007 = a009 + -1;
cur = cur - a007;
a007 = a008 + a007;
a005 = -1 - a005;
a005 = a009 - a002;
if (a005 > a003) {
a006 += a001;
a003 -= a007;
a000 = -3 - a005;
a006 = a005 + a001;
a007 = cur - a000;
a004 = a000 - a004;
a002 = a000 + a009;
a003 += a004;
a002 = a001 + a006;
a004 += a001;
a005 += a000;
a000 -= a003;
a003 = a005 - cur;
a003 -= a001;
a005 += 4;
cur = a003 - a000;
a007 += a002;
a001 = a005 + a004;
} else {
a003 = a005 + 2;
}
a009 += -5;
a006 = a002 - a000;
a001 += -4;
a005 = a004 - a006;
a009 += a000;
a003 = a002 + a000;
a000 -= a005;
a006 = a009 - a003;
cur = a008 + a007;
cur -= a005;
a003 = cur - -4;
a009 = a007 - 4;
a007 += cur;
a002 = a009 + a001;
a009 = a000 + a004;
a009 = -4 + a008;
a000 += a002;
} else {
a001 = a001 - cur;
a004 = a000 - a006;
a006 = a007 - a009;
a006 = -4 - cur;
a000 += cur;
cur = a002 - a005;
a005 = a009 + a003;
a006 -= a007;
a002 += cur;
a005 = a005 + a001;
a002 = a003 - a003;
a009 = a004 - a009;
cur += -4;
a001 = a009 - a007;
a007 = cur - a007;
a005 = a004 - cur;
}
a005 -= a000;
a007 -= -1;
a005 -= a006;
a008 = a009 + a002;
a006 = a005 - a008;
a000 -= a003;
a001 -= a003;
a009 += a008;
cur -= a000;
a003 = 2 + a007;
cur = a008 + cur;
a005 -= a007;
a008 -= -2;
a009 += a008;
a005 = a008 + a009;
a000 = a009 - a005;
a008 = cur + a003;
}
output.collect(prefix, new IntWritable(a006));
}
