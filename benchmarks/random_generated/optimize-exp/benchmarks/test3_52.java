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
a011 += a010;
a000 = -3 + a004;
a006 -= a010;
a009 -= a003;
a003 = 0 - a004;
a007 += a011;
a013 = a011 - cur;
a006 += a003;
a013 = a012 - a011;
a011 = a010 - a000;
a011 = cur - a003;
a010 = a000 - a010;
a001 -= -5;
a009 = a013 - a008;
a013 = a011 - a000;
a001 = a008 - a012;
a003 = a004 + a005;
a006 = a002 + a010;
a002 = a003 + a009;
a008 = a007 - a000;
a007 = a014 + a007;
a008 += a000;
cur += a007;
a010 = a007 + a001;
a011 = a004 + a011;
if (a002 <= a003) {
a011 += a010;
cur += cur;
a011 = a005 - a003;
a010 = a005 - a004;
a014 = a009 - a005;
a013 -= a001;
a004 += cur;
a013 -= a001;
a011 = a003 - a003;
a002 -= a004;
a001 -= a004;
a004 = a008 - a004;
if (a000 > a005) {
a000 -= a000;
a012 = a001 + a014;
a010 -= a006;
a000 = a006 - a005;
a001 = a013 - -4;
a013 += a013;
a006 += a013;
a014 = a001 - a006;
a007 += a012;
} else {
a005 = a000 + a003;
a011 = a011 - a009;
a013 = a003 - 1;
a012 = a008 + a005;
}
a000 = a000 + a012;
a007 = cur + a006;
a011 = a011 * 1;
a009 = a008 - a008;
a003 = a003 - a003;
a005 -= a001;
a013 = a014 - a000;
a000 = a007 - a000;
a014 += a006;
a002 = a009 - a010;
a007 -= a007;
a008 = a005 + a013;
a014 = a004 - a008;
a005 = cur + a007;
a012 = a012 - a011;
a010 -= a009;
a012 = a008 - a005;
a004 += a002;
a009 -= a000;
a012 = a005 + a000;
a011 = a012 - a007;
a009 = a006 - a006;
a006 -= a007;
a005 += a003;
a010 = a006 + a001;
a001 = a001 + a001;
a008 -= a010;
a004 += a000;
a012 += a008;
a001 = a012 + a005;
a002 -= a011;
a002 += a003;
if (a003 > -1) {
cur = a006 - a000;
a004 -= a004;
a009 += a009;
a007 = a002 + a010;
a013 += a014;
} else {
a002 -= a000;
cur = a014 + a011;
a005 = cur - a006;
a005 -= a002;
a000 = a013 + a006;
a014 = a004 - a005;
a003 = a005 + a003;
a014 = 2 + a014;
a008 = a001 + a012;
a000 -= a013;
a005 -= a005;
a001 -= a013;
a008 = a008 + a004;
a002 = a000 * 3;
a008 -= cur;
a014 = a007 + a000;
a011 -= a008;
a002 += a003;
}
a005 += a014;
} else {
a009 = a003 - a007;
a011 = a011 + a006;
a012 -= a002;
a002 = a012 - a003;
a009 -= a012;
a001 = cur - a000;
}
cur = a009 + a007;
a007 += a010;
a014 = a007 - a013;
a010 = a010 - a006;
a007 = a008 + a007;
a005 = a005 + a004;
a004 = a000 - a010;
a008 = a007 - a012;
a011 = a003 + 1;
a012 += a010;
a005 = a007 - a009;
a000 = a007 + cur;
a000 += a001;
cur -= a005;
a004 += a012;
a010 += a008;
a011 = a008 - 4;
a007 += a009;
a010 = a008 - a005;
a008 -= a011;
a010 = a001 - a000;
a010 = a000 - a004;
a002 = a000 + a001;
a001 = a013 - a013;
a008 = a014 - a008;
a002 += a003;
a008 -= a008;
a011 = a000 - a002;
a007 -= a005;
cur += a011;
cur = a003 - a008;
a003 = a008 - cur;
a008 -= a014;
cur = a005 + a005;
a009 -= a008;
a002 = a012 - a007;
a014 = a005 + a004;
cur = a007 - a006;
}
output.collect(prefix, new IntWritable(a014));
}
