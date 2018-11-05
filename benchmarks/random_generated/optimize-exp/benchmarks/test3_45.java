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
a014 = a012 - a007;
a000 -= a005;
a012 = a011 + -5;
a009 = a013 + -2;
a006 += a011;
a008 = a000 + a006;
a002 = a006 - a000;
a012 = a010 - a007;
cur -= a014;
cur += a007;
a006 = a009 + a012;
a001 += a008;
a003 = a011 + a004;
a006 = a004 + a010;
a013 += a000;
a005 += a004;
a002 = a008 - a004;
a008 = a002 + a005;
cur = a006 + a005;
a006 = a000 - 2;
cur = -2 - a012;
a010 -= a000;
a005 += a012;
a001 += a000;
a001 = a013 + a002;
cur = cur - a009;
a009 = a011 - cur;
a013 = a006 - cur;
a003 = a010 - a014;
a000 -= a012;
a002 -= a011;
if (a004 >= a013) {
a003 += a006;
cur += a005;
a006 -= a005;
a002 = 0 - a010;
a014 -= a014;
a012 = a011 + cur;
a002 = a000 + a011;
cur = a012 + a004;
a004 -= a011;
a004 -= cur;
a001 -= a001;
a013 = a010 + a006;
cur += -4;
a009 = a014 + a009;
a008 = a005 + a007;
a005 += a004;
a001 = a000 - a009;
if (a009 == a005) {
a009 -= cur;
a002 += a004;
a007 -= -4;
a013 -= a000;
a013 = a002 - a010;
a007 = a002 + a002;
a001 = a013 + a008;
a006 += a010;
a000 = a012 + a006;
a000 += a007;
a013 = a010 + a007;
a001 = a011 - a005;
a007 -= a013;
a007 += a012;
a013 = -5 - a006;
a004 = cur + a004;
a010 = a005 - a012;
a009 -= a013;
a001 = a000 + a006;
a006 = a009 + a014;
cur += a006;
a000 = a009 - cur;
a005 = a013 - a011;
a006 += a004;
a009 = a006 + a014;
} else {
a012 += a010;
a003 += -1;
a010 -= a003;
a000 += a001;
cur -= a004;
a002 = a003 + a010;
a014 += a007;
a002 = a007 - a014;
a014 = a013 + a004;
a006 = a001 + a014;
a009 -= a003;
a006 = a012 * -5;
a000 = a002 - a005;
a005 = a007 + a000;
a000 += a014;
a010 -= a013;
a009 -= a010;
a000 = a010 - a014;
cur += a013;
a010 -= a014;
}
cur = a010 + a011;
a008 -= a014;
a002 = a001 - a014;
a011 = a003 - a008;
a007 += a012;
a014 += a007;
a012 = a013 + -2;
a009 += cur;
a006 = a000 + a008;
a002 = a005 - a007;
a001 -= -2;
a003 -= a011;
a011 += a005;
cur = a012 + a003;
a013 += cur;
a003 -= a008;
a009 = a005 + a006;
a013 -= a005;
a013 = a004 - a005;
a001 -= a009;
a002 = a000 + cur;
if (a004 < a008) {
a007 = cur + a009;
a004 += 4;
a014 += 1;
a005 += cur;
a011 -= a003;
a005 = a001 + a007;
a002 = 2 - a009;
a011 = a004 - a009;
a014 -= a014;
cur = a000 + a000;
a009 = a009 - a003;
a009 += a004;
a010 += a006;
a010 -= 0;
a005 = a014 + a004;
a000 -= a011;
a004 = a000 + a007;
a010 = a005 + 2;
a000 = a001 + cur;
a001 = a004 + cur;
a009 += cur;
a002 = a013 - a004;
a010 = a008 - a005;
a011 = cur - a014;
a009 = a008 - a013;
a010 = a013 + 3;
a005 -= a002;
a009 += a010;
cur += a002;
a008 = -1 - a006;
a013 -= a013;
a000 -= a014;
a003 -= 0;
} else {
a010 = a014 + a008;
}
a010 -= a002;
} else {
}
a001 -= a005;
}
output.collect(prefix, new IntWritable(a010));
}
