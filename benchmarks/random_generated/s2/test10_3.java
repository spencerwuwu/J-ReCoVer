// Note: only +, - operations
// Parameters:
//   Variables:   10
//   Baselines:   100
//   If-Branches: 10

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
int cur = 0;

while (iter.hasNext()) {
cur = iter.next().get();
a005 = a000 - a008;
a005 -= a004;
a005 = a007 + a001;
a001 = a008 + a001;
if (a008 == cur) {
a004 = a009 + a006;
a001 -= a001;
a003 += a005;
cur -= a005;
if (a003 <= a008) {
if (a000 < cur) {
cur = a005 * 4;
a000 = cur - a000;
a005 = -2 - a003;
a004 -= a004;
if (a008 == cur) {
a000 = a007 + a000;
cur -= cur;
a000 = a006 + a008;
a005 += a001;
a000 += a004;
a009 -= a000;
a007 = a001 + a002;
if (a001 != a006) {
a002 -= a001;
a001 += a009;
a005 = a003 - a009;
} else {
a003 += a009;
if (a008 >= a003) {
a007 -= a007;
a002 = a008 + a004;
a009 = cur - a002;
a003 -= a004;
a003 = a009 - a004;
if (a004 >= a000) {
cur += a003;
a008 = a008 - a001;
a004 = 2 - -1;
a003 = a002 + a008;
if (a009 == a003) {
cur = a006 + a007;
a004 -= a003;
if (a005 < a004) {
cur = a003 + a002;
a005 += a007;
a009 -= a009;
a009 = a003 * -4;
a009 += a005;
a001 = a003 + 3;
} else {
if (a002 > cur) {
a008 += a007;
a003 -= 3;
a007 -= a001;
a004 -= a006;
} else {
a001 -= a007;
a002 = a002 + a007;
}
a001 += -4;
}
a009 -= a000;
a003 -= a002;
a002 = a005 - a007;
a000 += a000;
cur -= a005;
a001 = a003 - -5;
} else {
a002 -= -3;
a002 += a000;
a001 -= a007;
cur = cur + a006;
a002 += -1;
a000 = a001 - a002;
}
cur -= a002;
a007 = a003 - a007;
a001 = a001 + a007;
a001 -= a007;
a008 += a006;
a006 += cur;
} else {
a002 -= a003;
a003 -= a000;
a008 -= a000;
a007 -= a000;
}
a009 = a009 + a005;
a000 += a000;
a001 -= 1;
a007 = a005 - a004;
a006 -= -2;
a006 -= a002;
a009 -= a007;
} else {
a007 = a008 - cur;
a007 += a005;
}
a008 = a009 + a003;
a003 = a006 + a006;
a005 = a000 - a005;
}
a007 = a005 + 4;
cur += a009;
a002 -= a001;
a007 = a004 + a005;
a002 -= a009;
a002 -= a008;
} else {
a007 = a009 + a008;
a009 = -3 - a006;
}
} else {
a005 -= a001;
}
a009 += a000;
a002 = a000 + a005;
} else {
a001 += cur;
a006 += a008;
a006 = cur - -3;
}
a006 += a009;
a004 = a002 - cur;
a002 -= a007;
a005 = a000 + a003;
a001 = a000 - a007;
} else {
}
}
output.collect(prefix, new IntWritable(a006));
}
