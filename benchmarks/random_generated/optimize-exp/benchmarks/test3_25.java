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
a002 = a009 - a001;
a009 = a003 - a004;
a000 = a014 - a004;
a000 += cur;
a012 -= cur;
a002 += a011;
a007 = a003 + a005;
if (a004 == a014) {
a001 = a007 - a013;
a004 = a003 + a002;
a005 = a007 - a011;
a012 = a011 + a004;
a010 = a008 + cur;
a013 -= a000;
a007 += a014;
a006 += a004;
a010 = a003 + a007;
a012 = a004 * -5;
} else {
a001 += a014;
a012 = a001 + a004;
a000 = a014 + a012;
a005 = 3 - cur;
a012 = -5 - a011;
a009 = a012 + a005;
a009 += a008;
a007 = cur + 1;
a002 = a013 + a008;
a012 += a007;
a004 -= a003;
a003 = a006 + 1;
a008 += a002;
a006 = a007 + a002;
a008 = a005 - a003;
a003 = a011 - a007;
a001 += a009;
a006 -= a007;
a014 = cur + a006;
a009 = a005 - a004;
if (a009 != a006) {
a001 = a006 + a012;
a010 = a001 + a006;
a002 = a007 + a011;
a006 += cur;
a002 = a001 + a010;
a006 += a013;
a007 = 0 + a007;
a004 -= a004;
a003 = a003 + a009;
a012 = a007 + a009;
a011 -= a005;
a009 += a010;
a008 += a004;
a007 = a003 - a006;
a013 -= a004;
a010 = a000 + a011;
a008 = -4 + a003;
a002 += a013;
a012 -= a002;
a009 += a010;
a004 += -1;
a011 += a004;
cur = a006 - a002;
a002 += -1;
a011 += a010;
a006 += a011;
a007 = a013 + a002;
a014 = a009 + cur;
a008 = cur + cur;
a004 -= a003;
a013 -= a012;
a010 = a000 + a008;
a003 += a012;
} else {
a002 += a009;
a009 -= a002;
a007 = a000 - a010;
a000 = a014 - -2;
a007 = a007 - -2;
a002 = a005 + a009;
a003 -= a007;
if (a014 == a007) {
a010 = a009 + a004;
a003 = a002 + a007;
a003 = a006 + a010;
a008 = a011 + a004;
a003 = a008 - a006;
a011 = a009 * 4;
a011 = a011 - a009;
a003 -= a008;
} else {
a007 += cur;
a014 -= a002;
a003 = a008 - a014;
a010 = a010 - a012;
a014 = a002 + a000;
a003 += a013;
a013 += a012;
a001 -= 4;
a010 = a014 + a005;
a012 = a013 * -2;
a002 = a005 + a001;
a006 -= a002;
a002 -= a012;
a001 += a000;
cur = a004 + cur;
a006 -= a008;
a013 = a005 + a011;
a003 += a004;
a004 += 0;
a005 = a012 + a013;
a010 = a004 * 0;
a008 -= a004;
a002 = a009 + a002;
a006 += a002;
a009 = cur - -2;
a000 += a007;
cur += a002;
a012 = a007 + a007;
a011 -= a006;
a012 = a007 - a014;
a008 = a014 - a008;
a000 += a004;
}
a014 += a000;
a001 += a003;
a003 = a009 + a010;
a012 = a003 - a003;
a008 = a007 + a012;
cur -= a008;
cur = cur + -2;
a002 -= a013;
cur = a004 + cur;
a005 -= a014;
a011 -= a003;
cur = a012 - a001;
a014 += a003;
a014 = a008 - a008;
a012 = a011 + a009;
a011 = a005 + a007;
a006 -= a003;
a006 = a010 * 3;
a000 -= a006;
a001 = a002 + a006;
a009 = a004 - a000;
a012 += a014;
a009 -= a011;
a000 = a013 + a008;
a012 += a009;
a010 = a010 - a011;
a000 -= a014;
a004 += a006;
a008 -= a002;
a007 += a008;
}
a013 += 1;
a003 += a008;
}
a000 -= a003;
}
output.collect(prefix, new IntWritable(a012));
}
