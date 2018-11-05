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
a014 = a008 + a006;
a012 = cur + a003;
a002 = a003 + 4;
a009 -= a011;
a004 += a012;
a012 -= a003;
a008 = a005 - cur;
cur = 0 + a002;
a003 -= a008;
a011 = a003 - a001;
a010 = a010 - a001;
a003 = a009 - cur;
a003 += a002;
a001 -= a010;
a001 += a004;
a005 = a007 - a013;
a006 -= a013;
a001 = a000 + a005;
a013 = a003 + a003;
a014 += a005;
a011 = a010 - a010;
cur += 1;
a005 = a010 - a004;
a011 = a010 - a014;
if (a012 != a008) {
a008 = a003 - a012;
a014 = a007 - a006;
a013 += a007;
a012 = a005 + a012;
a005 = a008 - -1;
a014 = a013 + cur;
cur += a011;
a010 += a014;
a003 = a008 + a000;
cur = 0 + a009;
a013 += a000;
a003 = a010 + a003;
a001 = a011 + a011;
a010 = a000 + a006;
a013 = a012 + a010;
a009 -= a001;
a010 = a000 + a006;
a002 -= 0;
a000 -= a014;
a014 -= a001;
a008 = a002 - a010;
a006 += a013;
a011 = a006 - a008;
a010 = a007 - a012;
cur -= a003;
a003 += -2;
a008 -= a004;
if (a000 > a006) {
a002 = cur - a000;
a012 += a007;
a010 = a000 + a004;
cur = a014 * 3;
cur -= a013;
a006 = a009 + a001;
a012 = a007 - a002;
a014 = a006 + a014;
cur -= a000;
a011 += a011;
a001 += a004;
a006 -= a002;
a014 += cur;
cur += 1;
a005 -= a000;
a001 = a003 - a001;
cur = a009 + a013;
a014 += a009;
a004 = a001 + a000;
a001 = a011 + a006;
if (cur == a012) {
a004 = a005 + a008;
a013 -= a003;
cur = a006 - a000;
a005 = a002 - -1;
a001 = a000 + 4;
a002 -= a012;
a007 -= a012;
a001 = a002 + a010;
a010 -= a014;
a006 = a004 - a004;
a003 += a004;
a014 += a009;
a014 = -4 - a005;
a008 = a012 + a001;
a010 = a005 - a004;
a002 = a005 - a001;
a011 = a007 + a013;
a002 = a009 + a010;
a000 += a003;
a002 = -3 + a006;
a011 = a010 + a006;
a004 -= 1;
a008 -= a003;
a001 -= a002;
a013 = a002 * 1;
a004 -= a003;
a007 -= a010;
a009 = a006 + a002;
cur = a001 - a003;
} else {
a000 = a001 - a005;
a013 += a001;
a011 -= a010;
a011 += a012;
a009 = a013 - a004;
a014 = a007 - a008;
a003 = a008 - a013;
a002 = -3 - a002;
a003 = a007 - a009;
a013 = a000 - a011;
}
cur = a007 + a013;
a000 -= a002;
a011 = a006 + a001;
a010 -= a007;
a009 = a010 + 0;
a007 = 4 - a008;
a004 += a000;
a009 -= a005;
a011 = a013 - a002;
a008 = -4 + a002;
a013 = a007 - a014;
a003 = a006 - a014;
a014 += a012;
cur = a012 + a010;
a010 += a011;
a005 = a008 - a005;
a006 -= cur;
a000 -= a014;
a002 -= cur;
a013 = -1 - a011;
} else {
a008 = a008 + a012;
a002 = a005 + a010;
a000 = a000 - cur;
a012 += a008;
a007 = a007 + a007;
a006 = a002 + a007;
a005 += a001;
a007 = a005 - a012;
}
a013 = a001 - a014;
a008 -= a008;
a013 = a002 + a008;
a014 -= a014;
cur += cur;
a004 -= 4;
a001 = a002 - a006;
a009 -= a000;
} else {
a007 -= a001;
}
a012 = -4 + 2;
a011 = a013 - a004;
a000 += a000;
}
output.collect(prefix, new IntWritable(cur));
}
