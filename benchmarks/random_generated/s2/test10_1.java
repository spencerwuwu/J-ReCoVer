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
a003 = -3 + 0;
a009 = a001 - a009;
a008 = a009 + a003;
if (a000 >= -5) {
a003 = a007 - cur;
a004 = a003 + a003;
if (a001 < a003) {
a006 += cur;
a009 -= a004;
a000 = a004 - a006;
a005 = a008 - a001;
a001 += a008;
a002 = a003 * -4;
if (a001 >= a006) {
a007 -= a003;
a000 += a004;
cur = a005 - a003;
a009 = a008 + a009;
cur -= a002;
a008 -= a004;
} else {
a002 = 2 + a008;
a003 -= a007;
a000 = a000 + a007;
a002 += a006;
a003 -= a003;
a003 += a001;
a005 = a008 + a005;
}
a009 = a004 + a009;
a009 = a002 + a006;
a005 -= a000;
a008 += a009;
if (a002 <= a000) {
a009 = a005 - a000;
a005 = a009 - a004;
if (a005 > a009) {
a004 -= a004;
a003 -= a000;
a009 = cur + a007;
a002 += a000;
a002 -= a005;
cur += a004;
a009 = 3 - a006;
} else {
a006 = a009 - a005;
a006 += a006;
a008 = a001 - a005;
}
a005 = a005 - a008;
cur = a009 - a009;
a003 = a007 - a009;
a005 += cur;
a003 -= a005;
a005 = a002 + a008;
} else {
if (a006 != a005) {
a000 -= 0;
a007 = a005 - a004;
a003 = a006 - a000;
a005 += 4;
a006 -= a009;
a004 = a004 + a000;
a005 += a003;
} else {
a005 -= a005;
cur += a008;
a007 = a009 - a009;
if (a002 != a007) {
a006 = a009 + a000;
a008 -= a006;
a000 += a008;
a008 = 3 - a002;
a000 = 4 + a005;
a005 += a007;
a005 += cur;
if (a000 >= cur) {
a006 += cur;
} else {
a005 = a000 + a007;
a003 += a006;
a009 += a001;
a009 += a008;
a003 = 3 + a002;
}
a001 = -3 - a003;
a000 -= 1;
} else {
a005 = a005 + a000;
a005 = a000 - -4;
a001 = a009 - cur;
a009 = a003 - a009;
if (a004 < a008) {
if (a008 > a002) {
a000 = a003 - a006;
cur = a007 + a009;
a001 = a001 - a009;
} else {
cur += a001;
a009 = a002 - a003;
a006 = a002 + a009;
a007 += 0;
a003 = a000 + a002;
}
cur = a003 + a002;
a003 += a003;
a001 = a007 + a004;
a001 += a006;
a001 -= -2;
} else {
a007 -= a006;
}
a000 += a007;
}
a000 -= a006;
a000 -= a000;
a007 = a008 + a005;
a009 -= a003;
a002 = a004 + a006;
a001 = a004 * -3;
}
a001 = a006 + a001;
a007 = a005 + a004;
}
} else {
}
} else {
}
a009 = a003 + a002;
}
output.collect(prefix, new IntWritable(a008));
}
