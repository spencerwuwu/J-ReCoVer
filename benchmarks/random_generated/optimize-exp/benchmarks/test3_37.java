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
a012 = a008 + -2;
a005 = cur - a004;
a002 = a013 - 0;
a004 = a011 - cur;
a007 = cur + a008;
cur = a004 + a000;
a000 = a005 + a005;
a014 += a002;
a003 = a009 - a005;
cur -= a010;
a001 = a013 - a001;
a005 = a013 + a002;
cur -= a002;
if (a008 < a008) {
a000 = a003 + 0;
a009 -= a000;
a002 = a002 - a004;
a004 = a004 - a004;
a012 = a013 - a001;
a007 = cur - 4;
cur -= a011;
a010 += a004;
a008 -= a010;
a003 -= a013;
a012 -= a012;
a003 += a012;
a006 += a002;
a005 -= a011;
a011 = a008 - 0;
a003 -= cur;
a012 -= a014;
a011 += a004;
a014 = a002 - a004;
a004 -= a011;
a008 = a012 - a009;
a002 = a012 + a001;
a012 = a001 + a012;
a013 = a010 - a009;
a002 = a003 + a008;
if (a003 > a000) {
a006 += a008;
cur = 2 + a002;
a012 -= a002;
cur += a000;
a000 += a010;
a006 = a008 - cur;
a007 -= -1;
a003 = a014 - a013;
a013 = a012 - a004;
a000 = a001 - 0;
a011 -= a003;
a000 -= 4;
cur = a009 + a004;
a007 -= -1;
a006 -= a012;
a004 = a002 + a002;
a006 = a002 + a009;
a006 -= a005;
a006 = -4 - a006;
a002 = a014 + a006;
a003 -= a005;
a009 = a005 + a002;
a008 = a005 + a000;
a014 = a002 + a014;
a013 -= a013;
a004 -= a003;
cur += a007;
a013 -= a009;
a004 = a007 - a011;
cur += a000;
if (a003 >= a005) {
a003 = a008 + a001;
a002 -= a007;
cur += a006;
} else {
a010 -= a011;
a004 -= a003;
a007 = cur + a000;
a008 += a004;
a007 = a006 - a005;
a007 += a014;
a000 = a002 + a013;
a000 = a008 + a011;
a000 = a002 + -1;
a014 += 3;
a008 -= -3;
a010 = a007 + a013;
a006 = -5 + a007;
a006 -= a009;
}
a007 = a010 + a009;
a002 = a002 - a000;
a001 = cur + a009;
a008 = a014 + a002;
a008 = a006 + a002;
a002 -= a000;
a011 = a002 + a005;
cur -= a011;
a005 = 0 + a006;
a001 = a008 + a006;
a010 = a000 - a001;
a010 = a003 - a011;
a012 = a010 - a007;
a005 = a011 + a014;
a014 += a011;
a002 += a012;
a013 -= a010;
a001 = a002 + a002;
a003 += a005;
a002 += 0;
a011 = a009 + cur;
a014 -= a007;
a005 += a000;
a003 = a010 + a004;
a012 = a006 - cur;
} else {
a014 = cur + a004;
a001 = a008 + cur;
a010 += a001;
a009 = a013 - a007;
a004 -= cur;
}
a006 = -1 - a007;
a001 -= 2;
a004 = a011 + cur;
a007 = a004 + a005;
a008 -= a002;
a006 = a002 - a005;
cur -= a014;
a002 = a000 + a001;
a007 = a001 - a010;
a008 = a005 + a003;
cur = a006 - a011;
a009 += a006;
a001 -= a000;
a002 = a013 + -5;
a008 -= a003;
a008 = a006 - a001;
a013 -= cur;
a012 = a013 - a004;
a013 += a009;
a013 -= a007;
a011 = a010 + a009;
a012 += a010;
a006 = a004 + a005;
a012 = a000 - 1;
} else {
a011 += a000;
a013 -= a007;
a006 = a009 - a000;
cur = a007 + a013;
a006 = a002 - a014;
a014 = a013 + a008;
a001 += a012;
a008 = a007 + 2;
a000 += a004;
}
a007 += a000;
a002 = a004 - a011;
}
output.collect(prefix, new IntWritable(a002));
}
