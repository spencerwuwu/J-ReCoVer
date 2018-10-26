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
a002 = a007 - cur;
a009 = a009 + -2;
a000 = a009 + a003;
a008 -= a004;
a000 = a001 - a003;
if (a009 != a007) {
a008 = a004 - a005;
a001 -= a007;
a003 = a000 - a004;
a005 = a004 + a005;
} else {
if (a006 != a000) {
a000 += a005;
cur += 0;
if (a003 != a006) {
a000 = a000 - 0;
a003 += a006;
a000 = a007 - a006;
if (cur == a003) {
cur = a001 - a004;
a002 += -2;
if (a007 != a006) {
a008 -= a004;
a004 += a002;
a009 = a004 - a002;
a005 = a000 + a005;
a004 -= a008;
if (a001 != -2) {
a006 = a000 + a008;
cur = a008 + a005;
a008 = a007 + a000;
a003 += a002;
} else {
a001 += a002;
a000 = a002 - a008;
a000 = a004 - a004;
if (a005 <= a000) {
a003 = cur + a007;
a005 -= a008;
a001 -= a007;
if (a006 < a001) {
a000 -= 1;
a008 = 2 - a001;
a008 = a008 - a004;
a004 += a003;
a008 += a009;
a001 = a003 + a003;
a003 = 3 - a008;
} else {
a006 = a008 + cur;
a003 = a001 - a006;
a002 += cur;
}
a009 -= a003;
a003 = a007 + a005;
if (cur <= a006) {
a009 = a001 - a003;
cur = a008 + a001;
if (a005 >= 0) {
a006 = a001 + a006;
a005 += a004;
} else {
a008 = a005 + a008;
cur += a004;
a005 = a009 - 1;
}
a002 += a003;
a005 += a000;
cur = a008 - -5;
a002 += a009;
a009 = a007 - cur;
a005 = -5 + a005;
} else {
a003 -= cur;
a009 -= a007;
a003 = a004 - a000;
}
cur = a007 + 1;
a004 = a005 - a001;
} else {
a006 = a003 + a009;
}
a006 = a009 + a000;
}
a004 += a005;
a005 = a000 + a003;
a000 -= a009;
} else {
a000 = a008 + a000;
}
} else {
a006 += a005;
}
a001 = a004 + a003;
a005 = a005 + a006;
a001 = a006 + 3;
a007 = 3 - a007;
a003 -= -1;
a003 += a005;
} else {
cur += a004;
a009 = a000 + a009;
a000 = cur - a006;
a002 = a003 + a008;
}
} else {
}
a005 -= a003;
a000 -= a002;
a006 = a004 + a004;
a001 = a001 + a006;
a006 -= a002;
a001 = a003 - a000;
a001 = a004 + 2;
}
a006 -= a006;
a001 = a002 - a001;
a007 -= a008;
a002 += a006;
a001 = a003 + a005;
a005 += a009;
a009 += a005;
a001 = cur + a000;
cur += a004;
a008 += a000;
a003 = a007 + a002;
a003 += a009;
a008 -= a004;
a008 += -3;
a002 += a004;
}
output.collect(prefix, new IntWritable(a008));
}
