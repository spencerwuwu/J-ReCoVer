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
a000 += a001;
a006 = a007 + a002;
a000 = 0 + a008;
cur = a008 - a004;
if (cur > a001) {
if (a003 > a008) {
a004 += a007;
a006 = a000 - a006;
a008 += cur;
a004 = a001 + cur;
a004 = a005 - cur;
} else {
a009 = a003 + a002;
a005 += cur;
a009 = cur - a001;
if (a008 > 4) {
a004 = a003 + a008;
a003 += a000;
a003 = a004 - a009;
a001 = a008 + a003;
} else {
a002 = a009 - cur;
a005 -= a005;
a006 += a005;
a005 = a008 + 1;
}
a006 += a005;
if (a009 <= a002) {
a008 -= a004;
a005 += a005;
a003 -= cur;
a000 = a003 - a007;
} else {
cur = a002 - a003;
a001 = a000 - a005;
a004 = a001 + a004;
a009 = -1 + a003;
}
}
a009 = a008 + a008;
a001 = -2 - a007;
a005 = a009 - a005;
a002 = a005 - a009;
a006 += a003;
a007 -= a004;
} else {
a008 = a009 - a002;
if (a001 <= 2) {
a001 = a006 - a005;
a005 += 3;
a002 = a002 - a003;
a006 += 1;
cur = a008 + a008;
if (a004 != a005) {
a009 -= cur;
a007 = a001 + a002;
a001 = a000 - a005;
if (a008 > 1) {
a004 = cur + a009;
a004 = a007 - a002;
} else {
a000 = a002 + a008;
a007 = 3 + cur;
a005 += a002;
a006 = 3 - -3;
a009 = cur - a006;
a007 = a005 - a006;
cur = a003 + a000;
if (a000 >= 0) {
a003 += a007;
a008 += a000;
a008 -= a001;
a001 += a003;
cur -= a001;
a009 = a000 + a006;
} else {
}
a002 += a007;
a006 = a004 + a001;
}
a007 = a006 - 4;
cur = a004 + a009;
a008 = a007 - 4;
a008 -= a002;
a005 += a005;
a002 = a002 + a003;
a002 += a007;
if (a004 < a006) {
a001 -= a004;
a002 = 2 + a003;
} else {
a003 = a001 + a001;
}
} else {
a002 -= a007;
a001 = a001 + a009;
a005 -= a005;
a008 -= a008;
a003 = a007 - a000;
if (a009 < a009) {
a004 = a001 + cur;
cur += a008;
a000 += a007;
} else {
a005 = a005 - a007;
cur = a001 + a002;
}
a006 -= cur;
a005 = a001 + a002;
a005 -= a007;
a009 = cur - a005;
}
a009 = a000 + a008;
a008 -= -1;
a005 += a006;
} else {
a003 = a009 + a003;
}
a006 = a002 - -1;
cur += a002;
a005 = a000 - a000;
a001 += a003;
a003 -= a008;
cur -= a007;
a005 = a003 + a000;
}
a005 += a005;
a005 += a002;
a008 += a007;
a002 = a003 + a009;
}
output.collect(prefix, new IntWritable(a007));
}
