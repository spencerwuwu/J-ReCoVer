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
a004 = a007 + a002;
a005 -= -1;
a004 = a001 - a009;
if (a008 <= a005) {
a007 += a002;
a003 = a007 + a007;
a008 -= a009;
a007 = a003 - a007;
} else {
a007 += a003;
a001 -= 1;
a007 = a006 + a003;
a004 += a008;
a008 = a009 + a000;
}
a003 = a004 - a009;
a003 = a002 + a002;
a003 = a004 + a009;
a009 = -1 - a003;
a008 = a004 + a008;
cur = a003 + cur;
a001 = a009 + a006;
if (a007 <= a002) {
a007 = -4 - a005;
a009 -= a009;
} else {
}
a004 = a009 - a005;
a002 += a007;
a001 += a002;
if (a007 <= a002) {
a006 = cur - a007;
a003 = a006 - a000;
if (cur == a000) {
a001 += -5;
a004 = a001 - a003;
} else {
a002 = a007 - a007;
a002 += a006;
a005 = a002 + cur;
a000 = a005 * 0;
}
a004 = a004 - a006;
a002 += a007;
cur = -1 + a001;
a003 = cur - a005;
if (a006 < a000) {
cur -= a009;
a000 = a002 - a000;
a005 = cur + a000;
if (a008 == -3) {
a008 = a009 + a001;
a006 += a007;
a006 -= a006;
if (a005 >= a009) {
if (a009 <= a004) {
a006 = cur + cur;
if (a002 != a002) {
a000 += a009;
if (a007 != a005) {
a006 += 3;
a001 -= a000;
a007 -= a001;
} else {
a003 = a003 - cur;
a005 = a002 + a002;
a008 -= a001;
a009 -= cur;
a003 = cur - a000;
cur = -3 + a007;
}
cur = a006 + a004;
a007 += a004;
a003 += a003;
a009 = a001 + a006;
a000 = a006 - a008;
} else {
}
a009 -= a006;
a000 = a006 + a008;
a002 += a000;
a006 += a009;
a008 = cur + 2;
} else {
a007 -= a002;
a005 = a008 + 4;
}
a000 = a005 - cur;
a000 += a009;
} else {
}
a004 += cur;
cur = a008 - a000;
a000 = 2 - a001;
cur = cur - cur;
} else {
a009 = cur - cur;
a001 += a004;
a003 = a005 + a006;
a008 += a004;
a000 = a009 + 0;
a004 = a006 + cur;
}
a009 -= 1;
a008 = cur - a000;
a006 -= a001;
} else {
cur -= a003;
a002 = a009 - 1;
cur += a005;
a002 -= a009;
a008 = a004 - a004;
}
a007 += 3;
a006 -= a000;
} else {
}
a000 -= a007;
a004 = cur - a007;
cur += a003;
a003 -= a001;
a003 += a001;
a002 -= a007;
a002 += a006;
a005 += a008;
a000 += a002;
a007 += a001;
a000 = a008 + a007;
a000 = a005 + -2;
a004 = cur + a008;
}
output.collect(prefix, new IntWritable(cur));
}
