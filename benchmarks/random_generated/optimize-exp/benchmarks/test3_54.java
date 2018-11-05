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
cur -= a004;
a004 = a002 + a000;
cur = a011 + a007;
a009 -= a003;
a008 += a000;
a006 -= cur;
a005 -= a006;
cur += a003;
a008 = a005 - a011;
a000 -= a013;
a006 -= a009;
a007 += cur;
a007 += a001;
a012 = a000 + a004;
a001 -= a003;
a007 += a006;
a002 = a000 + a005;
a009 = a001 + a000;
a009 = a012 - a009;
a006 += a011;
if (a010 == a002) {
a010 -= a013;
a013 = a004 + a001;
a000 = a014 - cur;
a011 -= a004;
a013 += a012;
a012 -= a008;
a010 = a005 - cur;
a003 = a010 + a011;
a007 = a008 - a001;
a000 = a006 - a001;
a011 = a000 - a011;
a013 = a000 - a013;
a014 -= a002;
a000 = 1 + a000;
a014 += a008;
a013 += a006;
a011 += a007;
if (a011 >= a007) {
a014 = a001 - a005;
a013 = a006 + a005;
a013 += a010;
a001 += a005;
cur = a013 - a012;
a011 = cur + 2;
a002 += a014;
a006 = a005 + 3;
a014 = a006 + a000;
a013 += a008;
a009 = a002 - a007;
a006 += a008;
a004 += a001;
a000 -= a010;
a007 = a003 + a000;
a008 -= a006;
a002 = a012 - a004;
a012 = a000 - a014;
} else {
a008 = a013 + a003;
a005 = a005 + a008;
a001 -= a011;
a005 = a004 - a013;
a009 += a009;
a012 -= a009;
a001 += a012;
a014 -= a004;
a012 = a002 - a006;
a006 = a014 + -1;
a014 = -3 + a014;
if (a001 >= 2) {
a003 -= a005;
a010 -= a013;
a007 = a013 - a012;
a005 += a002;
a007 += a013;
a004 = a014 + a009;
a009 = a004 + a011;
a003 = a013 + a013;
a002 = a011 + a000;
a010 = a009 - a000;
a008 = a005 - a002;
} else {
a005 -= a009;
a006 += 3;
a014 = a003 + a005;
a006 = a000 - a008;
}
cur = a006 - a012;
a011 = a013 + a002;
a005 = a003 - a012;
a013 += a008;
a009 += a002;
a008 = a005 + a010;
a006 = a010 - a005;
a011 -= cur;
}
cur = a007 + a013;
a008 = a013 + a005;
a009 -= a000;
a002 -= a006;
a003 -= a000;
a006 += a013;
a008 -= a012;
a001 += cur;
} else {
a009 = cur + a001;
a000 -= a001;
a000 += a002;
a001 -= a011;
a008 -= -1;
a002 += a003;
a000 = a005 + a010;
a001 -= a003;
a014 = a011 - a005;
a014 = a006 + a006;
a010 -= a012;
a003 = a010 + a014;
a014 = a003 - a012;
a000 += a007;
a001 -= a009;
a004 -= a007;
a013 -= a001;
a000 += a002;
a000 = a014 + 2;
a010 = a014 + a002;
cur -= a004;
a005 = a005 - a014;
a001 -= a000;
a002 -= cur;
a001 = a010 - a014;
a000 = a011 - a009;
a009 = a006 - a004;
a005 = a000 + a000;
a007 -= a012;
}
a002 -= cur;
cur += -5;
a013 += a010;
cur += -2;
a009 = a014 - a009;
a002 += a012;
a009 -= cur;
a009 -= a014;
a014 = cur - a001;
a004 = a006 - cur;
a001 += a010;
a000 = a001 - a012;
a008 = a008 + a009;
a014 = a000 + a007;
a002 += a004;
a012 = a011 - a004;
cur += a006;
a009 += a006;
a011 -= a014;
a007 += a003;
a000 -= a010;
a007 += cur;
a000 = a005 - a000;
a010 -= a007;
}
output.collect(prefix, new IntWritable(a012));
}
