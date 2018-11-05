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
a011 = a000 - a008;
a001 = a014 - a003;
a007 -= a011;
a005 += a003;
a013 -= a013;
a009 = a009 + a003;
a007 += a005;
a013 = a001 - a011;
a001 = a013 - a011;
a004 = a002 + a013;
a011 -= a014;
a014 = a005 - a004;
a000 = a011 - a010;
a009 -= a010;
a010 = a010 - a012;
a010 = cur + a013;
a013 += a001;
a001 += a012;
a003 += a001;
a010 = a010 - a007;
a014 = -3 - a006;
a014 -= a001;
a007 += cur;
cur = cur - a013;
a010 -= a003;
a002 = a013 - a013;
a004 += a009;
a011 += a001;
a013 = a012 - a013;
a013 = -1 - a004;
if (a012 == a000) {
a006 += a005;
a012 = a013 + a004;
a007 -= a012;
a003 = a001 + a005;
a013 = a003 + a008;
a014 = a011 + a002;
a005 = a013 - a002;
a006 = a000 + a001;
a013 += a009;
a000 -= a013;
a000 = a009 - a009;
a000 = a003 + a001;
a011 = a009 + a005;
if (a006 != 1) {
a012 = cur + a007;
a002 = a010 - a008;
a009 -= a009;
a010 += a000;
a014 -= a005;
a014 = a007 + a006;
a008 -= 4;
a002 = a012 - a011;
cur += a008;
a011 = a001 - a009;
a002 -= -5;
a007 -= a005;
a013 -= cur;
} else {
a014 -= a012;
a001 = a005 * 4;
a000 += a013;
a006 -= a003;
a001 = a013 + a008;
a000 += a009;
cur = a008 - a004;
cur = cur - -3;
a000 += cur;
a012 -= a013;
a010 -= a009;
a003 = a008 + a000;
a011 -= a004;
a008 += a001;
a006 = a013 - a006;
a009 = a001 - a007;
a002 = a013 - cur;
a007 = a010 - cur;
a004 = a007 + a006;
}
a004 = a013 - a014;
a013 = a000 + a007;
a007 = -4 - cur;
cur = a013 + a000;
a012 += a004;
a000 -= a013;
a006 = a003 - a005;
a005 = a011 + a013;
a001 -= a003;
a010 -= a007;
a011 += a010;
a013 += -5;
a009 -= a012;
a013 = a012 - cur;
a001 = a003 - a000;
a000 += a002;
a008 += a011;
a013 -= a010;
a013 -= a000;
if (a012 < a010) {
a000 = a012 + a014;
cur += a010;
a005 = a007 + a012;
cur = a001 - a000;
a001 -= a010;
a012 -= a012;
a013 += a011;
a014 -= a010;
cur -= a013;
a003 -= a005;
a009 -= a011;
a014 = a008 - a012;
a000 += a004;
a012 = -3 + a008;
a009 -= a006;
a002 -= a010;
a010 += a007;
a011 += a011;
a000 -= a001;
a012 = a006 - a013;
a006 += a004;
a002 = a008 - a006;
} else {
a010 -= a008;
a006 += a012;
a012 += a008;
a012 = a012 - a002;
a010 += a004;
a011 = a000 - a014;
a005 += a002;
a002 = a011 - a012;
a008 = a010 + a007;
a006 += a011;
cur = a011 - a010;
a009 -= cur;
a004 -= a013;
a000 -= a002;
a002 += -3;
a003 = a007 + a007;
a009 = a014 + a000;
a000 -= a011;
a009 -= a009;
a000 -= a005;
}
a011 += a008;
a000 = a000 - a002;
a004 = a013 + -4;
a013 = a011 + a005;
a009 = -1 - a006;
cur = a006 + a011;
a011 = a001 - a012;
a010 = a005 + a007;
a002 -= a012;
a012 -= a013;
a012 = a005 - a010;
a001 += -1;
} else {
cur -= a006;
}
a012 += a010;
}
output.collect(prefix, new IntWritable(a009));
}
