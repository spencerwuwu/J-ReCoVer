// Note: only +, - operations
// Parameters:
//   Variables:   15
//   Baselines:   150
//   If-Branches: 3

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
int a010 = 0;
int a011 = 0;
int a012 = 0;
int a013 = 0;
int a014 = 0;
int cur = 0;

while (iter.hasNext()) {
cur = iter.next().get();
a002 -= a006;
a010 = a004 - a008;
cur += a007;
a006 += a002;
a005 += a000;
a006 = a012 - a001;
a006 = a010 - a005;
a006 += a011;
a008 -= a002;
a003 = a000 - a009;
a008 = a013 - cur;
a000 = -5 + cur;
a006 += a010;
a002 += a009;
a004 = cur - a005;
a014 = a004 - cur;
cur = a007 - a009;
if (a012 < a009) {
a004 = a002 + a010;
a007 = a004 + 1;
a003 -= a012;
a012 += a013;
cur = a001 - a006;
if (a003 > a005) {
a014 += cur;
a006 = a012 * -5;
a014 += a011;
a013 = a008 + a013;
cur += a006;
cur = a002 - a012;
a000 = a011 + a005;
a001 = a005 + a009;
a004 = a004 - a013;
a005 = a004 + a006;
a006 = a003 + a007;
a007 -= a014;
a004 = a009 + a006;
cur = a009 - a003;
a011 = a013 - a003;
a009 = a010 + a010;
a003 = a011 * -2;
a001 -= a014;
a003 += a004;
a006 += a000;
a009 = a009 - a003;
a001 += a003;
cur -= cur;
} else {
a004 = a005 + a000;
cur = a013 - a001;
a010 -= a014;
a014 = a004 - a011;
a002 = a004 - 4;
a006 = a011 - a014;
a001 += a002;
a010 = a009 - a000;
a013 = a000 + a013;
a000 -= cur;
a006 = a008 + a014;
a008 -= a009;
a008 = a005 - a001;
if (a014 < a011) {
a008 -= a009;
a003 += a012;
a003 -= a007;
a012 = -1 + a014;
a004 = a008 * 1;
a007 = a000 * -1;
a001 = a008 - a002;
a003 += cur;
a006 = a008 - a004;
a006 -= a010;
a004 = a004 + a005;
a007 = a010 + a012;
a006 += a003;
a008 += a004;
a013 = -3 - a012;
a002 = a011 + a002;
a007 -= a010;
a014 -= a001;
a012 += a003;
a000 -= a003;
cur += a000;
a008 = a012 + a005;
a005 += a005;
a007 -= a000;
a003 -= a006;
a012 = a004 - a008;
a014 -= a000;
cur = a004 - a000;
a001 += a014;
a006 += a000;
} else {
a002 += a011;
a004 = a008 - a008;
a005 = a013 - a005;
a000 += a005;
a001 += a011;
a008 = a002 - a007;
a012 -= a011;
a010 = cur + a011;
a002 = a014 - a009;
a006 -= a003;
a008 = a003 + a004;
a013 -= a012;
a008 -= a005;
cur += a011;
a004 -= -3;
a005 = a001 - a000;
cur += a014;
a010 = a003 - a005;
a005 = a007 + a005;
a006 += a000;
a008 += a002;
a009 = a005 + a004;
a003 += a009;
a014 -= a000;
a009 = a003 + a005;
}
a009 -= a011;
a004 = a006 - a000;
a011 = a010 + a003;
cur = a008 - a013;
a014 = a013 - a003;
a002 -= a011;
a011 = a009 - a006;
a000 = a005 - a001;
a011 = a010 + a008;
cur = a006 - a012;
a004 = 1 - a009;
a014 += a012;
a001 += a010;
a007 += a013;
a002 = a010 - a009;
a003 = a009 - a007;
a001 -= -1;
a006 = 0 + a009;
a012 -= a013;
a000 -= a010;
}
cur = a001 - a007;
a002 -= a007;
a001 = -3 + a000;
} else {
a008 -= a005;
cur = a003 - a009;
a002 = a004 - a013;
a008 = a011 - a008;
}
a001 = a004 + a005;
a008 = a007 - a006;
a012 -= -4;
a001 += a013;
a004 += a003;
a006 = a002 + a014;
a003 = a012 + a006;
a008 = a001 - a007;
cur += a001;
a001 -= a012;
}
output.collect(prefix, new IntWritable(a005));
}
