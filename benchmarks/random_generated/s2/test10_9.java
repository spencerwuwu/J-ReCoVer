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
a007 -= a004;
a002 = a004 - a005;
a000 = a005 * -3;
a001 = a004 - a000;
if (a002 >= -2) {
cur -= a001;
a004 += a006;
a000 = a002 + a001;
a007 -= a007;
} else {
}
if (a002 == -5) {
a003 -= a003;
a008 -= a005;
a009 -= a005;
a000 = a006 - a007;
a005 = -3 + cur;
a003 = a009 - a007;
} else {
a009 = a003 - a006;
if (a008 == a004) {
a008 = a001 - a003;
a003 -= a001;
if (a001 != a008) {
a004 = a002 + a000;
a005 -= a003;
a000 = a006 + a003;
if (a004 != a003) {
a000 += a008;
a007 -= cur;
cur -= a001;
a001 -= 3;
a003 += a006;
a007 = a008 + a006;
a009 += a007;
} else {
a009 -= a000;
a004 -= a007;
cur = 2 - a000;
a001 -= a007;
}
a007 = a004 - a004;
a005 = a002 + a003;
a006 += a005;
} else {
if (a006 >= a005) {
a000 = a003 + a007;
a008 += a006;
a008 -= a008;
cur += a002;
a007 = 0 - -5;
a007 = a000 + a007;
a004 -= a008;
if (a006 >= a007) {
a003 += a006;
if (a002 != a001) {
a004 -= a001;
cur = a007 + a001;
a009 -= a003;
a005 -= cur;
a005 = a001 - a008;
if (a003 >= a007) {
a008 = a009 - a006;
a008 += a006;
a009 = cur + 2;
a001 = a001 + a001;
if (a003 <= a003) {
a006 = a009 + a006;
a003 += a003;
a009 = a006 + a002;
cur = a001 * 4;
a007 = a007 - a007;
} else {
a002 -= a005;
a009 += a006;
a006 += a000;
a009 = a001 - a002;
a001 += a003;
}
a005 -= a000;
} else {
a002 += a008;
a007 = a009 + a000;
a008 -= a008;
}
a001 = a004 - a002;
a004 = a002 + a001;
cur = a001 + cur;
a002 -= a006;
} else {
a007 += a007;
a008 = a006 + a007;
a001 -= a006;
a007 += a003;
a009 -= 0;
a007 -= a009;
}
a001 += a007;
a005 = a007 + a005;
a007 = a009 - a000;
} else {
a007 = a007 + a003;
a000 += a001;
cur = a007 + a006;
}
a007 -= a003;
cur += a006;
a003 = a000 - a009;
} else {
a001 += -3;
}
a000 = a009 - a003;
a002 = a002 - a009;
a000 -= a000;
cur -= a009;
}
} else {
a007 = a007 + a000;
a007 += a008;
a002 -= a000;
}
a001 = a003 - a001;
a005 += a007;
}
a005 = a006 + a004;
a005 -= cur;
a007 += a008;
a006 = a007 + a003;
a001 += a002;
a006 -= a008;
}
output.collect(prefix, new IntWritable(a008));
}
