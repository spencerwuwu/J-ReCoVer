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
a001 -= a003;
a009 = -3 + 4;
if (a006 >= a011) {
a001 = a001 - a012;
a006 += a012;
a011 = a001 - -4;
a000 = a010 + a011;
a004 = cur - a012;
a012 -= a005;
a009 = cur + a004;
a004 -= a011;
a000 += a001;
a007 = a001 - a000;
a005 -= a013;
a011 = a010 - a000;
a012 = a011 - a008;
a008 += a006;
a006 -= a005;
a003 += cur;
a013 = a009 - -1;
a011 += 2;
a006 = a002 + -2;
a001 -= a002;
if (a000 < a013) {
a003 = a002 + a000;
cur -= a013;
a004 = a007 + a004;
a012 = 3 - a004;
a001 += a005;
cur = a000 + a000;
a014 -= a010;
a010 += a010;
a014 = a003 - a007;
a011 = a011 - a005;
cur = a002 + a007;
if (cur != a012) {
a014 += a007;
a002 = a014 + a006;
a006 = a000 + a001;
a004 = a006 - a005;
a010 += a002;
a001 = a009 - a009;
a001 = a014 - cur;
a011 = a003 + cur;
a012 += cur;
a005 += a006;
a008 = 0 + a004;
a002 += a001;
a000 = cur - a002;
a008 = a005 + -1;
a007 = a009 + a010;
cur -= cur;
a009 = a001 - a009;
a013 += a010;
a012 = a011 + a011;
} else {
a013 = a003 - a012;
a003 += a004;
a010 -= a009;
a002 -= a002;
a012 = a001 + a009;
a009 += a005;
a011 -= cur;
a002 += a004;
a012 += a002;
a007 += a003;
a002 += a002;
a009 += -4;
a006 = a001 + a009;
a005 -= a009;
a000 = a014 - a010;
a001 = a000 + a011;
a001 = a012 + a000;
}
a000 = a012 - a000;
a004 = cur - a004;
a005 = a010 - a010;
a014 = a014 - a002;
a000 = a011 - a013;
a000 = a014 + a010;
a014 -= a004;
a014 -= a002;
a011 -= a006;
a012 = a002 + a010;
a010 += cur;
a003 -= a013;
cur = a002 - a000;
a001 += a002;
a004 += a014;
a014 = a004 + a000;
a013 = a009 + a000;
a008 = a009 + a004;
a008 -= a007;
a012 -= a014;
a004 += a000;
a010 -= a006;
a006 = a014 + a002;
a006 += a003;
a013 -= a011;
a004 += cur;
cur += a004;
} else {
}
a013 = a001 - a008;
a013 = a010 - a000;
a003 = a009 - a014;
a004 = a001 + a009;
a000 -= a005;
a002 += a006;
a000 = a012 + a001;
a011 = a013 - a014;
a000 = 3 - a001;
a013 = a013 + a013;
cur -= a013;
a007 += a007;
a014 += a004;
a002 = a002 + a014;
a014 += a008;
cur = a014 - a010;
a000 += a003;
a012 -= a006;
a010 -= a007;
a009 = a005 - a000;
a008 -= cur;
a012 = a000 * -4;
cur -= a007;
a001 += a002;
a009 -= a003;
a007 = cur - a001;
a009 = a014 + a010;
a001 -= a001;
a012 += a013;
} else {
a007 -= a005;
a005 = a013 + a014;
a013 = a003 - a012;
a004 = a011 + a011;
a012 += 2;
a007 = a010 - a005;
a008 = -3 + a014;
a001 -= a003;
a006 = a004 - a014;
a010 = a004 - -3;
a010 = a010 + a011;
a000 += a005;
a013 = a007 - a004;
cur = a004 - a007;
a013 = a012 + a011;
a008 = 4 + a006;
a007 = a006 + a005;
a014 -= a005;
a003 = a009 - a002;
a013 += a000;
a004 -= a000;
a013 -= a013;
cur += a007;
}
a008 = a004 + a012;
a012 = a014 + a007;
}
output.collect(prefix, new IntWritable(a007));
}
