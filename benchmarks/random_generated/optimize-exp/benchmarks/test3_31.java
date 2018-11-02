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
a006 = a010 - a008;
a002 = a012 - a014;
a007 = a003 - a013;
a000 -= a006;
a011 += a006;
a011 += a013;
a002 -= a014;
a006 += a007;
a010 += a012;
cur = a006 - a012;
cur = a011 - a009;
a008 += a006;
if (a011 >= a010) {
cur -= cur;
a008 += a010;
a006 = a010 + a001;
a001 = a014 + a002;
a007 -= a010;
a014 -= a006;
a009 = a001 - a002;
a007 -= a008;
a007 = a003 + a000;
a000 -= a008;
a008 = a007 + a007;
a010 -= a003;
a014 -= a006;
a008 += a008;
a002 += a002;
a001 += a013;
} else {
a000 -= a002;
a002 = a014 - a006;
a003 = a013 + a007;
a002 = -4 - a003;
a005 -= a014;
a009 += a014;
a005 = a007 + cur;
a003 = a000 - a004;
a008 = a001 - a013;
a003 = cur + a012;
a006 = a003 + a012;
a006 += a006;
a014 -= a007;
a010 -= a003;
a012 = a013 + a009;
a008 = a005 - a012;
a010 -= a003;
a009 = a000 * 3;
cur -= a005;
}
a004 = a000 - a009;
a004 = a011 - a003;
a007 = a001 + a014;
a012 += a014;
a012 = a004 + a006;
a008 += a013;
a005 = a001 - a014;
a009 -= a009;
a004 = a014 * 2;
a006 = a004 + a014;
a013 = a002 + a007;
a006 = a004 - a000;
a000 += a010;
a004 -= a013;
a004 = 2 - a012;
a001 = a002 + a009;
a001 = a009 - -2;
a014 = a002 - a008;
if (cur != a012) {
a012 = a009 - a013;
a014 -= 0;
a010 += a003;
a013 += a005;
a008 += a008;
cur -= a000;
a007 = a006 - a008;
a004 = a005 - a012;
a002 += a007;
a003 -= a011;
a005 += a010;
a005 -= cur;
a014 += a002;
a011 = a002 + a007;
a007 = a007 + a001;
a002 += a013;
a013 = a008 - a011;
a005 = a008 - a014;
a004 = a011 - a012;
a012 = cur + a002;
a011 = a012 + a011;
a002 = a005 + a011;
a012 = a008 - a011;
a014 = a003 - a005;
if (a004 < a006) {
a012 = a003 - a006;
a004 = a013 - cur;
a010 -= a006;
a011 = a007 + a004;
a010 = a014 * 4;
a012 += a007;
a005 = a002 - a004;
cur += a005;
a006 = a013 + cur;
a002 = a010 - a001;
a010 -= cur;
a005 = a006 + a005;
a006 = a006 + a000;
a001 = a009 - a010;
a005 = a004 + a010;
a014 -= a001;
a009 += a004;
a009 += a001;
cur = a014 + a004;
a007 -= a009;
a008 -= a010;
a014 = a004 - a003;
a010 = a005 - a002;
} else {
a000 -= a001;
a000 -= a006;
a006 -= a002;
a010 += a003;
}
a011 = a002 - a005;
a013 = a011 + a013;
a002 = a000 - a014;
a002 += a007;
a002 -= cur;
a013 = a012 - a008;
a014 = a006 + a010;
a002 = a005 + a001;
a012 -= a004;
a012 += 2;
a011 += a008;
a009 = cur + a003;
a004 += a002;
cur -= a001;
a001 = a000 - a008;
a010 -= a004;
cur = a010 - a013;
a013 = 2 - a002;
a011 = -5 - -4;
a011 = a006 + 2;
} else {
a001 = a014 - a006;
a013 = a011 + a002;
a014 -= a001;
cur -= -4;
a007 = cur + a003;
a013 += a004;
a002 += a006;
}
cur = a010 + a001;
a003 -= a007;
a005 += a010;
a014 -= a007;
a006 = a009 + a000;
cur += a003;
a011 -= a003;
}
output.collect(prefix, new IntWritable(a004));
}
