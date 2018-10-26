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
a002 = a006 + a002;
a007 = a001 - cur;
a008 -= a005;
if (a002 < a002) {
a008 = a004 - a004;
a007 -= a001;
if (a001 != a009) {
a005 = a006 - a000;
a000 += a003;
a002 = a002 - a005;
a002 += -2;
if (a000 < a006) {
a007 += a006;
a009 -= a004;
} else {
a002 = a002 - a009;
a003 += a003;
a000 -= a006;
cur += a001;
a001 -= a006;
if (a007 == a002) {
a002 = a000 + a008;
a001 = a007 - a005;
a006 -= a004;
a004 = a006 + a005;
a007 += a005;
cur += a008;
a007 += cur;
if (a004 > cur) {
a004 += a005;
a005 += cur;
a009 += a000;
a009 += a009;
a008 -= cur;
a001 = a008 + a008;
if (a009 <= cur) {
a002 = a002 + cur;
a004 = a004 - a002;
a004 -= a001;
a008 = a001 + a009;
a008 = a007 + a004;
} else {
a008 = a005 + a005;
a000 -= a007;
a005 += cur;
a009 -= -2;
if (a002 <= a008) {
a009 = a006 + a007;
a009 -= a008;
a000 = a006 + a009;
if (a001 <= a004) {
if (a003 == a000) {
a005 += a002;
if (a005 == a005) {
a007 = cur + a003;
a009 = a005 - a000;
} else {
a000 = a009 + a005;
a001 += a001;
}
a004 = a006 - a007;
a006 = a009 - a004;
a001 = a002 - a007;
a007 = cur - 3;
} else {
a009 += a002;
}
a006 -= a008;
a007 = a005 - cur;
} else {
a006 = a001 - a005;
}
cur = a001 + a007;
a004 += a004;
a006 -= a007;
} else {
a004 += a001;
}
a001 = a009 + a007;
a008 = a002 + a007;
a003 = a002 + cur;
a008 -= a005;
}
} else {
a009 -= a005;
a003 = a009 + a006;
a000 -= -1;
a006 = a000 + a003;
a002 += a001;
cur -= a008;
a008 -= a006;
}
a004 = a003 - a009;
a007 += a004;
a006 -= a004;
a006 = a009 + a003;
} else {
a001 = cur - a007;
a002 += a007;
a007 -= a008;
a006 += a001;
a000 = a009 - -3;
a009 += a000;
a006 = a001 - a002;
}
cur = cur - a002;
a001 = a005 - a004;
a006 += a007;
a002 += -3;
a007 += cur;
cur += a003;
}
a001 = a001 + a007;
a007 += a001;
a004 += cur;
a001 -= a004;
a003 = a000 + a006;
} else {
cur = a007 + 1;
a002 = a002 - a009;
a008 = a005 - a002;
cur -= a005;
a009 = -4 + a008;
}
} else {
cur = a002 + a002;
a004 += a004;
}
cur -= 1;
cur = a002 + a009;
}
output.collect(prefix, new IntWritable(cur));
}
