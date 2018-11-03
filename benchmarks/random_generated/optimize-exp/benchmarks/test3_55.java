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
a014 -= a002;
if (a005 <= a010) {
a006 += a004;
a011 -= a006;
a008 -= a010;
a004 = a005 - a005;
cur = a002 - a005;
a003 -= a000;
a004 = -4 + a010;
a006 -= a014;
a006 = a001 - a006;
a000 -= a010;
a010 -= a014;
a013 = a005 * -4;
a009 = a008 + a011;
a004 -= a002;
a005 = a002 - a011;
a004 = a011 - a003;
a005 += a001;
a004 += a003;
a007 += a011;
a003 += -1;
cur += a004;
a011 -= a001;
a002 = a007 - a011;
a003 = a012 + cur;
a003 = a008 + a006;
a010 -= a006;
} else {
a002 += a010;
a005 += a005;
a009 += a013;
a004 += a012;
a006 = a005 - a005;
a003 = a003 - a002;
a007 += a014;
a012 -= a002;
a008 = a010 - a011;
a011 = a009 + a001;
a003 += a010;
cur -= a002;
a014 = cur + a010;
a005 = a013 - a005;
a007 = -2 + 4;
cur += a009;
cur -= cur;
a002 += a007;
a013 -= a005;
a001 += a014;
if (cur != a001) {
a012 = a010 + a004;
a013 = a002 + a011;
a004 = cur - -4;
a005 -= a006;
a012 = a006 + a011;
a013 -= a013;
cur -= a005;
a003 -= a005;
a007 = a004 + a006;
a010 = a000 + cur;
a001 += a009;
a006 = a003 + a004;
a000 = a011 + a006;
a002 -= a010;
a014 += a003;
a010 += a008;
a012 = cur + a014;
cur = -2 - a012;
a006 = a005 - a001;
a014 = a000 + a010;
a007 = a005 - a007;
cur = a014 + a006;
a009 -= 4;
a009 += a005;
a010 += a014;
a002 += a001;
a014 = a010 - a005;
a005 -= a010;
a002 += a000;
a001 += cur;
a007 = -2 - a009;
a004 = a007 + a010;
} else {
a013 = a012 + 4;
a005 = a001 - a014;
a003 -= a012;
a009 += a010;
a009 = a013 + a006;
a009 = 4 + a006;
a004 = a012 + a007;
a002 = a004 - cur;
a006 -= a001;
a001 -= a001;
a012 = a009 - a009;
a005 -= a002;
a009 += a004;
a014 = a008 - a013;
a014 += a010;
a006 = a002 - cur;
cur = a010 - a005;
a003 -= a012;
a012 = a002 - a001;
cur += -4;
a001 += a007;
a000 = a010 + a010;
a008 = a003 - a006;
}
a000 = cur - a008;
a006 -= a012;
a007 += a014;
a004 -= a008;
a008 += a010;
a003 += a000;
a004 = a011 - a007;
a009 += a012;
a001 += a006;
a011 += a006;
a012 += a004;
a006 += a012;
a008 = a002 * -5;
a004 += a013;
a005 = -3 - cur;
a002 = a005 + a006;
a001 -= a001;
a003 -= a002;
a010 += a007;
a006 -= a000;
a007 = a005 - a010;
a008 -= a007;
a007 += cur;
a007 = a011 - a000;
a001 = cur - a010;
a011 -= a004;
a013 = a004 + a003;
a002 = a001 - a011;
a011 = a013 + a012;
a001 = a010 + a012;
a005 = a005 + a013;
a010 += a002;
}
a006 = a011 + a002;
a001 += a011;
a014 = a003 + a005;
a006 = a005 + a010;
a014 += a000;
a002 = a014 + 3;
a010 -= a013;
a006 = a002 - a012;
cur += a004;
a014 -= a007;
a012 = a013 - a012;
a002 = a010 + a013;
if (a001 >= a013) {
a009 -= a001;
} else {
a001 = a000 + 2;
}
a008 = a005 - a013;
a004 = a008 - a014;
}
output.collect(prefix, new IntWritable(a014));
}
