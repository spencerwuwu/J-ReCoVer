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
a005 = a007 - a001;
a001 = a009 - a011;
a005 = a012 + a014;
a006 -= a007;
a000 -= a000;
a014 -= a011;
a012 += cur;
a011 += a007;
a005 = a010 + -1;
a003 += cur;
a005 = a003 - a001;
a004 = a003 - a009;
a014 = a002 + 3;
a011 += a014;
a006 = a007 + a013;
a000 -= a012;
a009 += a000;
a014 += a004;
a011 += a000;
if (a012 >= a011) {
a006 = a010 + a012;
a004 = a009 + a011;
cur += a001;
a011 = a003 - a002;
a002 = a001 + a007;
a000 = a014 - a011;
a014 = a007 + a013;
a014 = a005 + a003;
a009 = cur + a004;
a006 -= a012;
a013 += a001;
a014 = cur * 1;
a008 = a011 - a008;
a009 = a003 - a005;
a009 = a011 + a006;
a001 = a014 + a009;
a012 = a013 + a007;
a013 += a012;
a003 -= a003;
a008 = a000 - a004;
a000 -= a000;
a003 = a004 + a004;
a006 = a002 * -4;
a014 = a011 - a009;
a002 = a006 - a006;
a007 += cur;
a004 -= a001;
cur -= a003;
a007 = a002 - a008;
} else {
a005 += a005;
a004 += a010;
a013 += a012;
a008 = a000 - a000;
a003 -= a006;
a014 = a008 + a009;
a014 = -5 - a013;
a008 = a008 + a013;
a010 -= a012;
a003 = a003 - a002;
a007 += a009;
cur = a003 - a005;
a004 = a002 + a011;
a012 = a010 - 0;
a000 = a003 + a001;
}
a013 += a006;
cur -= a004;
a005 -= a013;
a012 -= a000;
a012 = a010 + a005;
a002 = a005 + a014;
if (a000 < a014) {
a007 -= a007;
a005 -= cur;
a004 = a002 - a012;
a007 -= a010;
a007 = a004 - a008;
a009 += a007;
a006 = a005 - a001;
a000 = a006 - -1;
a002 += a000;
a004 = a001 - a014;
a010 = a001 - a007;
a001 = a014 + a008;
a009 = a004 - a008;
a008 -= 4;
a010 = a001 - -4;
a004 = a011 - -5;
a009 += a004;
a011 = -4 + a002;
a008 = a010 - a013;
cur -= 2;
a010 = a002 - -4;
a011 += a002;
a014 -= a011;
cur -= a011;
a010 = a012 + a011;
a003 += a009;
if (a005 < a008) {
a007 += a014;
a000 += a002;
a008 += -2;
cur = -5 - a010;
a004 -= a005;
a008 += a005;
a001 += a007;
a001 = a005 + a003;
a004 += a002;
a000 -= a012;
a014 = a006 + a002;
a013 = a009 - a003;
a014 = a007 - a006;
} else {
a004 -= a009;
a003 = a014 - a008;
a010 += a005;
a005 = a010 - a011;
}
a009 = cur + a014;
a000 += a004;
a003 = a012 + a007;
a014 = a011 - a013;
a001 = cur + a010;
a006 = a006 - a003;
a007 = a002 * -5;
a011 = cur - a011;
a012 += a009;
a009 = a006 + a012;
a001 = a002 - a000;
a009 += a008;
} else {
a004 = a006 + cur;
a010 -= a003;
a012 = a001 + a010;
a011 += a009;
a006 = a007 + a007;
}
a006 = a014 - a010;
cur = a004 + cur;
a003 = a004 + a004;
a006 += a006;
a003 -= a012;
a005 += -3;
cur = cur + a004;
a000 = a011 - a003;
a004 += a003;
a014 += a011;
a000 += a001;
a010 = a001 - a000;
a002 -= a004;
a011 -= a011;
a013 -= a006;
a002 = -1 + cur;
a002 = 3 + a003;
a005 += a000;
a014 += cur;
a009 -= a009;
a014 = a006 + a014;
}
output.collect(prefix, new IntWritable(a011));
}
