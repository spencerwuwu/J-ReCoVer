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
if (a007 < a012) {
a010 = a008 - a012;
a012 -= a008;
a009 = a008 + a007;
a009 = a006 - a000;
a012 = a012 + a007;
a006 += a014;
a014 += a011;
a000 += a008;
a010 = a011 - a014;
a013 += -4;
a014 -= a009;
a004 = a006 - a006;
a001 = cur - cur;
a006 -= a001;
a013 = cur - a006;
} else {
a004 -= a007;
a013 += a000;
a000 = a001 + a008;
cur = a004 + cur;
a005 = a012 + a011;
a001 = a006 + a014;
cur -= a012;
a006 -= a004;
a009 -= cur;
}
a007 -= a013;
a010 = a012 + a001;
a004 = a010 + a005;
a013 = a007 - a001;
a006 -= a001;
a010 = a011 + 3;
a007 = a008 + a012;
a011 = a013 + a003;
a006 -= a005;
a004 += a000;
a013 = a000 + a006;
a000 += a006;
cur = a005 + a013;
a014 = a013 - a007;
a013 -= cur;
a013 = a002 * -4;
a006 = a009 + cur;
a012 += a002;
if (a010 > 1) {
a007 += a007;
a013 -= cur;
a014 -= a008;
a010 = a001 - a010;
a002 += a009;
a005 = a004 + a010;
a011 = a007 - a006;
a013 = a009 + cur;
a012 = a003 - a003;
a008 -= a013;
a002 = a001 + a001;
a004 -= a009;
a006 -= a014;
a013 = a014 + a004;
a004 += a007;
a006 = a004 - a010;
a007 = a013 + a009;
a006 += a001;
a014 += a007;
a004 = a014 + a003;
a009 = cur - a001;
a004 -= a013;
a000 = a001 - a010;
a012 = cur - a000;
a003 = cur - a009;
a008 = -1 + a006;
a013 += a012;
a001 -= a008;
a002 = a008 - a002;
a008 = a001 + a006;
a011 = a006 + a002;
a002 = a001 + a007;
if (a003 >= a004) {
a000 += a012;
cur += a003;
a004 = a007 + a006;
a001 -= a012;
a005 = a002 - a010;
a013 = a005 + a007;
a003 = a011 + a003;
a000 -= cur;
a013 -= cur;
a003 = a009 - a006;
a012 += a008;
a003 += a004;
a007 -= 3;
a000 = a013 - a012;
a005 -= a012;
a004 = a014 - a003;
a006 = a006 - a013;
a010 = a004 + a012;
a013 = a003 + a003;
a006 -= a014;
} else {
}
a004 = a007 - a010;
a000 -= a010;
a001 = a000 - a008;
a014 = a010 + a013;
a002 -= a013;
a011 = a008 - a001;
a012 = a009 - a013;
a004 -= a007;
a010 += a001;
a003 = a013 + a011;
a007 = a010 - a008;
a002 += a006;
a011 = a006 - a001;
a006 += a004;
a010 = -4 - a000;
a013 -= a002;
a006 = a000 - a009;
cur = a010 - a011;
a006 -= a007;
a008 += a013;
a008 -= a010;
a013 += a002;
a002 += a014;
cur -= a008;
a012 = a010 - 1;
a009 += a012;
a002 -= a001;
} else {
a010 += a001;
a004 -= a014;
a000 += a000;
a004 = a000 + 2;
a000 = a006 - a009;
a003 -= 2;
a010 = 3 - a012;
a013 -= a003;
a001 += a003;
a011 -= a008;
a002 = cur - a009;
a002 -= a009;
a010 = cur - a010;
a005 -= a003;
a001 = a008 - a011;
}
a009 += a012;
a001 = a008 + a002;
a010 = 3 - a004;
a010 = a012 + a012;
a012 -= a006;
a003 = a014 - a010;
a012 += a000;
cur = a012 + a010;
a009 = a005 - a014;
a011 = a009 - a004;
a006 += a005;
a005 += a006;
a011 -= a010;
a000 += a003;
}
output.collect(prefix, new IntWritable(a002));
}
