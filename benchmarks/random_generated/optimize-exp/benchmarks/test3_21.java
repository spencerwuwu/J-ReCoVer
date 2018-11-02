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
a001 = 2 + cur;
a005 = a011 + a003;
a011 = cur - a014;
a002 += a014;
a003 = a013 + a012;
a004 = a012 + a012;
a002 = a006 + a001;
a011 -= a008;
cur += a010;
cur -= a001;
a009 = a003 - a014;
a012 += a000;
a007 += a011;
a010 -= a012;
a002 += a005;
a003 += a010;
a001 += a014;
a001 -= a001;
a000 += a000;
a004 = a002 + a013;
a002 = a008 - a007;
if (a004 >= a012) {
a007 = a011 - a008;
} else {
a008 -= a004;
a007 = cur - a001;
a013 -= a006;
cur = a009 - a004;
a008 = a010 + a012;
a000 += a007;
a003 = a006 - a002;
a003 = a002 * 1;
a003 -= a003;
a000 -= a014;
if (a014 == a003) {
a009 = a000 - a004;
a011 += 4;
a000 = a009 + a013;
a007 = cur + a000;
a003 = a003 - -5;
a003 = a007 + a008;
a014 += a008;
a008 = a001 + a001;
a012 += a002;
a007 = a008 + a009;
} else {
a004 -= cur;
a010 += a009;
a013 = a000 + a002;
a004 = a005 + a011;
a006 = a009 + a004;
a008 += a013;
a010 += a003;
a002 = a011 - a010;
cur = a002 + a002;
a003 += a012;
a007 -= 0;
a008 = a002 - a006;
a007 = a002 + a005;
a007 -= a002;
a005 = a010 - a013;
a001 = a012 - a014;
a011 = a011 + a014;
a014 = a012 + a003;
a002 = a006 - a011;
a002 = a002 - a013;
a001 -= a006;
a011 = a001 - a003;
}
a011 = a013 + a000;
a006 = a002 + a007;
a008 -= a006;
a008 = a014 - a014;
a011 = a003 - a012;
a004 = a006 - a012;
a012 = a008 - cur;
a011 += a001;
a007 = a014 - a014;
cur = cur + a003;
a004 -= a002;
a004 = a005 - -2;
a009 -= a012;
a007 = a003 + a014;
cur += cur;
a011 = a014 - a014;
a001 -= a002;
a003 = a001 - a001;
a011 -= a008;
a008 = a004 - a006;
a005 -= a010;
a010 += a002;
a005 -= a012;
a004 = a009 + a006;
a010 = a014 + a004;
a004 = a013 + a004;
a009 = a011 + a009;
a011 = a000 - cur;
a003 = a004 + a014;
a012 += a006;
}
if (a002 != a002) {
a010 += a003;
a014 += a004;
a006 -= cur;
a009 = -2 + a003;
a014 = a009 - cur;
a004 -= a003;
a010 = 4 + a005;
a005 = a010 + a014;
a001 = a006 - a014;
a007 = a011 - a013;
a010 = a007 - a008;
a004 += cur;
a002 = cur - a003;
a006 = a009 - cur;
a008 = a010 - a013;
a002 = cur - a008;
a009 += a001;
a006 = a014 + a002;
a013 += a014;
a009 = a008 - a007;
} else {
}
a011 -= a011;
a004 = a010 + a012;
a006 -= a011;
a012 -= a013;
a005 -= a002;
a006 -= a004;
a005 = a003 + a013;
a011 -= a003;
a011 -= a011;
a011 -= a002;
a000 = a003 + a004;
a014 = a007 - a001;
a013 += a004;
a004 = a004 + a007;
a008 -= a012;
a002 = a014 * -2;
a010 += cur;
a007 = 1 - a005;
a011 = a000 - a004;
cur = a002 - a009;
a000 -= a003;
a011 = a003 + a012;
a012 = a010 + a003;
a001 += a009;
a000 = a004 + a009;
a007 = a011 - a010;
a006 += a013;
a013 = a007 + a003;
a012 += a001;
a006 -= a001;
a000 = a004 - a006;
a005 += 1;
cur += -4;
a002 = a004 + a012;
a001 = a004 + a005;
a002 += a012;
}
output.collect(prefix, new IntWritable(a001));
}
