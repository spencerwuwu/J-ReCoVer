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
a000 = a002 - a001;
a002 = a002 - a003;
a009 = a008 - a009;
if (cur > a001) {
cur -= a007;
a004 += a007;
a002 = a001 + a000;
a008 = a009 - a008;
if (a001 > a002) {
a008 = a008 - a003;
if (a008 <= a002) {
a004 += a007;
cur += a000;
a001 -= a008;
a006 = a009 - a007;
a007 -= a008;
a000 = a000 - 4;
} else {
a002 += cur;
a000 = a000 + a009;
}
cur += cur;
a002 -= a005;
a000 += a000;
a008 -= 2;
a000 -= a008;
if (a007 != a008) {
a002 = 1 - a005;
a001 -= a002;
cur -= a002;
a002 += a007;
} else {
a003 = a004 + 0;
a001 -= a009;
a001 = a003 - a008;
a001 += a007;
cur -= a009;
a009 = a003 - a007;
if (a007 < a007) {
if (a009 >= 1) {
a006 -= a009;
a009 = a008 - a001;
a003 = a007 + cur;
a006 = a006 - a008;
a003 = a002 - a000;
a005 -= a000;
} else {
a000 += 1;
a000 = a001 + a001;
}
a005 += a001;
a000 -= cur;
a005 = a008 - a007;
if (a005 > a001) {
if (a006 > a004) {
a008 += a005;
a003 -= a008;
a008 += a002;
a005 = a008 - a004;
a001 += a007;
} else {
cur = a003 + a001;
a007 = a005 + a009;
a005 -= a007;
a000 += a001;
a003 -= -1;
if (a001 > a009) {
a001 = a003 - a007;
a002 -= a009;
a003 = a008 - a006;
} else {
a000 = a005 + a005;
a009 -= a002;
a004 += a002;
a003 += a005;
a007 += a002;
a003 = a000 + a005;
}
a003 += a000;
cur += cur;
a009 -= a004;
if (cur > a002) {
} else {
a001 = a003 + a002;
}
a002 = a005 - a007;
}
} else {
a008 -= a008;
}
a009 = a002 + cur;
a007 -= a007;
a001 -= -1;
a009 = a004 + a009;
} else {
cur -= a004;
a005 = a006 - a008;
cur = a001 + a009;
a003 = a002 + a008;
a008 = a005 + a004;
}
cur += a000;
a002 = a005 - a005;
a003 = a001 + a006;
a007 = a004 - a006;
a006 -= a001;
a007 += a001;
a001 -= 2;
}
a005 = cur - 1;
a006 += a000;
a005 += a004;
} else {
}
a009 = a004 + a001;
a003 -= a008;
} else {
a008 += a008;
a003 = a004 + 4;
a008 = a007 - a001;
a006 += a002;
a005 += a007;
a004 = a009 + a009;
}
a007 += a009;
a009 += a004;
a009 = a008 + a007;
a006 = a004 + a004;
a009 += a008;
a000 -= -1;
}
output.collect(prefix, new IntWritable(cur));
}
