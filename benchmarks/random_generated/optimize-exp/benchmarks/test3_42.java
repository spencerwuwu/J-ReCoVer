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
a009 += a001;
if (a014 != 1) {
a014 = a012 - a010;
a001 -= a001;
a008 -= a004;
a011 = a012 + a007;
a014 += a005;
a012 += a014;
a012 = cur + a000;
a011 -= -2;
a014 -= 0;
a008 = a014 - a012;
a011 += a005;
a001 = a002 + a001;
a012 = a003 + a004;
a013 = a005 + a002;
a012 = a013 + a009;
a001 += a003;
a005 = a002 + a003;
a001 -= a012;
a009 = a009 - a004;
a006 += a003;
a002 = a010 + a013;
a000 -= a007;
a004 = a012 - a010;
a000 = a008 - a002;
a002 -= a011;
a004 = a004 + a007;
cur += a005;
if (a002 > cur) {
a014 += -2;
a012 += a004;
a004 = a001 + a006;
if (a002 <= a009) {
a002 += a014;
a011 = a004 - a007;
a006 = a009 + cur;
cur -= a006;
a003 -= a008;
a005 += cur;
a010 += cur;
a000 = a004 - 0;
a004 -= cur;
a014 = a002 - a004;
a003 = a012 - a009;
a008 = a009 - a010;
a012 -= a008;
a010 -= cur;
a014 += cur;
a006 = a004 - a001;
a009 = a006 - a009;
a007 += a005;
a010 = a008 + a005;
a005 -= a008;
a005 = a001 + a009;
a012 += a014;
} else {
}
a004 -= a007;
a004 = a009 + a014;
a005 -= a010;
a001 -= cur;
cur -= a003;
a011 -= a009;
a003 -= a009;
a014 = a010 + -1;
a013 = a006 + a008;
a000 = a005 - a005;
a008 = a000 + a009;
a010 -= a001;
a004 = a011 + a007;
cur += a010;
a003 += 1;
a001 -= a009;
a012 = a008 + a004;
a003 = a000 + a003;
cur = a009 - a013;
a005 -= a003;
a009 = a006 - a001;
a004 = a008 - a010;
a011 -= a001;
a006 = a005 + cur;
a011 -= 3;
a004 += a004;
a013 = a004 * -1;
a010 -= a011;
cur = a014 + a004;
a012 = cur + a001;
a001 -= a002;
cur += a004;
} else {
cur = a004 - a012;
a005 = a005 - a000;
a000 -= a011;
a000 += a007;
a006 = cur + a004;
a014 = a004 - cur;
a004 = a008 - cur;
a009 -= a012;
a000 = a002 - a008;
a009 = a006 + a008;
a005 += a012;
a007 += a007;
a011 = cur - a013;
cur = cur + a005;
a004 = a001 - a011;
a005 = a012 + a007;
}
a004 = a014 + a007;
a007 += a005;
a006 = a001 + a003;
a007 = a002 - a010;
a010 = a001 - a006;
a010 += a012;
a001 = a002 - a012;
a002 += a007;
a013 += a000;
a014 -= a008;
cur = a003 - a008;
a003 = a006 + -4;
a013 = cur + -2;
a004 -= a007;
} else {
a004 = a010 + a009;
a003 = a007 - cur;
a006 -= a006;
a014 += a003;
a001 -= a013;
a003 = a002 - a006;
a001 = a000 + a002;
a013 -= a011;
}
a006 -= a010;
a013 += a011;
a011 += a008;
a010 = a000 + a000;
a002 += a001;
a008 = a003 + a011;
a007 += a004;
a003 = a002 - a012;
a002 = a000 - a000;
a000 = a008 + a014;
a012 -= a013;
a001 += a014;
a003 = a003 - a012;
a008 += a003;
cur += a000;
a000 = a003 - a002;
a002 -= a012;
a007 -= a014;
a010 = a008 + a008;
a012 = a007 - a006;
a007 = a014 - a014;
a005 -= a011;
a012 += 4;
cur = a009 + a007;
a007 = a004 - a009;
a013 = a003 + a010;
a013 -= a003;
}
output.collect(prefix, new IntWritable(a002));
}
