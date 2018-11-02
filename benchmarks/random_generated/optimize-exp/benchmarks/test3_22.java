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
a000 -= -1;
a002 -= a000;
a014 = a004 + a003;
a008 = a008 + a010;
a002 = a009 + a001;
cur += a007;
a005 = a004 + a012;
a011 = cur + a012;
a012 += a010;
a011 += a006;
a006 = a000 + a012;
a000 = a013 + a004;
a002 += a012;
a006 += a000;
a010 = a001 * -3;
a008 = a008 + cur;
a013 -= a003;
a002 = a005 + a012;
a010 = a007 - a007;
a002 = 3 - cur;
a007 = 4 + a002;
a001 = a004 - -5;
a010 += cur;
a003 = a004 + a008;
cur += a005;
a008 += cur;
a005 = a010 + a008;
a002 += a008;
a007 -= a004;
a001 = a013 * -5;
a010 -= cur;
a005 = a013 - a002;
a006 -= a014;
if (a005 != a000) {
cur -= a009;
a001 = a000 - a011;
a008 = a008 - a009;
a013 = a000 + a001;
a010 += a005;
} else {
a004 = a013 + a004;
a014 += cur;
if (a009 <= cur) {
a010 -= a013;
a007 = a005 - a000;
a001 += -3;
a010 = a009 + a003;
if (a009 < a003) {
a002 -= a000;
a003 += a011;
a009 = a006 + a010;
a007 -= a005;
a014 = a000 + a002;
a005 -= a010;
a004 = a012 + cur;
a004 = a002 + a014;
a010 -= a012;
cur = a009 + 0;
a001 = a014 + a010;
a011 = a007 + -2;
a011 = a011 * -2;
} else {
a013 = a002 + a010;
cur -= a010;
a010 -= a000;
a002 += a007;
a010 -= a005;
a014 += a003;
a013 = a004 - a009;
a012 = a012 - a014;
a002 -= a004;
a014 = a007 + a010;
a003 = a010 + a011;
a011 = a013 + a009;
a000 -= cur;
a000 = cur * -1;
a005 -= a000;
a001 = a002 + a003;
a005 = a014 - a004;
a012 = a003 - a014;
a012 = a006 * -2;
a006 += a012;
a002 = a003 - a005;
a010 = a014 + 1;
a007 -= a008;
a012 = a005 - a012;
a012 -= a002;
a010 += a007;
a001 = a001 + a004;
a007 += a011;
}
a014 += a006;
a001 -= a013;
a007 += a000;
cur = a002 - 1;
a012 -= a011;
a004 -= a004;
a005 = a005 + a002;
a008 = a014 + a004;
a001 += cur;
} else {
a000 = a004 - a011;
a013 += a009;
a014 += a013;
a006 += a011;
a011 = a008 - a003;
a000 = a007 - a005;
a006 = a003 - a014;
a011 = a003 + 2;
a004 += a009;
a010 = cur - a014;
a010 = a012 + a010;
a007 = a005 - a004;
a012 += a005;
a002 = cur + a006;
a006 = a006 + a014;
a003 = a002 - a013;
a001 -= a013;
a003 = a014 - a013;
}
a004 += a009;
a009 += a008;
a011 -= a010;
a014 = a009 - a000;
a006 += a014;
a008 = -3 - a010;
a010 = a012 + a008;
a007 = a012 - a003;
cur -= a007;
a012 -= a012;
a014 += a010;
}
a008 += a005;
a008 = cur - a005;
a011 -= 1;
a003 = cur - a014;
a012 += a008;
a010 -= a013;
a005 += a010;
a014 = a011 - a010;
a001 = a001 - a010;
a006 = a003 - a008;
a014 += -4;
a013 -= a000;
a010 = a013 - cur;
a014 = a009 + a006;
a001 = a004 + a010;
a002 -= -2;
a012 -= a000;
a006 += a003;
a005 -= a003;
a008 -= a008;
a001 += a003;
a010 -= a013;
a002 = a003 - a005;
a013 = a014 - cur;
a011 -= a014;
cur += a009;
a013 = a004 + a006;
}
output.collect(prefix, new IntWritable(a000));
}
