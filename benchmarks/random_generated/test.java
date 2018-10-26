// Note: only +, - operations
// Parameters:
//   Variables:   7
//   Baselines:   200
//   If-Branches: 10

public void reduce(Text prefix, Iterator<IntWritable> iter,
         OutputCollector<Text, DoubleWritable> output, Reporter reporter) throws IOException {
double a000 = 0;
double a001 = 0;
double a002 = 0;
double a003 = 0;
double a004 = 0;
double a005 = 0;
double a006 = 0;
double cur = 0;

while (iter.hasNext()) {
cur = iter.next().get();
a003 = a000 - a004;
a004 += cur;
a005 -= a006;
a004 = a002 + cur;
if (a001 == a001) {
a003 -= a001;
a003 -= a000;
a006 += a004;
a001 -= a001;
a006 += cur;
a001 = a006 - a004;
if (a005 <= a002) {
a006 += a006;
a005 = a004 - a006;
a001 = a001 + a004;
a006 = 3 - a003;
a003 = cur - a000;
a006 += a001;
a006 = 4 - a004;
a002 = a003 + 2;
cur = a000 + a005;
a005 -= -3;
a006 = a002 - a006;
cur = a004 + cur;
a004 = a002 + a006;
if (a002 <= a002) {
a005 = a003 - -4;
a000 = a005 + a005;
a006 = a006 + a002;
cur -= a006;
a002 -= a006;
a003 -= a005;
a001 = a004 - a005;
a005 = a002 + cur;
if (a003 >= a003) {
a002 += cur;
a002 = a002 - a006;
a003 = a006 + 0;
a001 = a002 + a005;
a001 = cur - a001;
a003 = a002 - a002;
a002 = a001 - -5;
cur -= a000;
a002 = a004 + a003;
a005 = a006 - a001;
a001 -= a004;
a002 -= a000;
if (a001 < a000) {
a003 -= a001;
a000 = a005 - a000;
a006 += a004;
a006 = cur - a006;
a001 = -1 + a002;
a006 = a005 + a006;
a002 = a004 - 2;
} else {
a002 += a001;
a000 = a005 + a002;
a002 -= -3;
a004 += a004;
cur = a002 + a001;
a005 = a006 - a004;
a000 -= cur;
a006 = 3 - a000;
if (a000 < a001) {
cur = a001 + a003;
a001 = a001 + a001;
a001 = -1 - a000;
a000 = a000 + a006;
if (a006 > a005) {
a002 = a002 + cur;
a000 -= -1;
a004 = a004 - -5;
a005 = cur * 1;
a005 = a006 + a002;
a000 += a003;
} else {
a004 += a005;
a006 = cur - a000;
a003 -= a001;
a000 -= cur;
a000 = a001 - a002;
a003 += a006;
a003 -= a006;
a005 -= a006;
a003 = a006 - 4;
a003 += cur;
a006 = cur - a001;
cur -= a000;
}
a006 = a003 - a002;
a000 -= a005;
a003 = a003 + cur;
a000 += a000;
if (a001 != a003) {
a003 += a001;
cur -= a003;
a006 -= 1;
a000 = a006 - 1;
cur -= a003;
a000 = a004 + a006;
} else {
a004 = a005 - a003;
}
cur += a003;
a003 = a003 + a000;
} else {
a005 -= a000;
a000 = a002 - a006;
if (a004 > 2) {
a001 = a000 - a003;
a003 = cur - a004;
if (cur < cur) {
} else {
a003 = cur + a005;
cur = a000 - a002;
a006 += -1;
a004 = 4 + a002;
a005 += a005;
a000 += a005;
}
a002 = a002 + a002;
a004 += -5;
a006 -= a000;
a006 = -3 - a005;
cur += a002;
a004 -= a006;
a005 = a001 + -5;
a005 = a000 - a001;
a005 -= a000;
a002 = a001 + a006;
a005 = a006 + a005;
a004 += cur;
} else {
a002 -= a005;
cur -= a005;
a000 -= a002;
a005 = a000 - -1;
a002 = cur - -3;
cur -= a005;
a001 = a004 - a003;
}
cur = cur - a003;
a000 -= a006;
cur = a004 + -1;
a000 = a000 + 4;
a001 = cur + 1;
a005 -= a000;
a002 += a001;
a005 = a003 + a004;
a000 = a000 - a002;
a004 = cur + a006;
a004 += a002;
a001 += a002;
cur = a006 + a005;
}
a001 = a004 + a000;
a005 -= a003;
a000 += a000;
cur = a004 - a006;
a006 -= -4;
a003 = a003 + a001;
a006 -= a001;
cur -= a000;
a004 = a005 + cur;
a001 += a002;
}
cur = a003 - a005;
a004 += a001;
a005 = cur + a003;
a006 += a001;
a002 += a001;
a002 -= a006;
a003 -= a000;
a003 += -2;
a004 = a006 - a006;
a006 = a006 + a003;
a000 = a006 + a001;
a006 = a004 + a003;
a006 = cur - cur;
} else {
a005 = a003 - a000;
a001 += cur;
a002 += a004;
a000 -= a002;
cur -= a002;
a005 += a003;
a005 = a002 - a006;
cur = a001 - a000;
a006 = cur - a002;
}
a000 -= a001;
a000 = a002 + a002;
a005 -= a001;
a006 = a002 + a004;
a004 -= -2;
} else {
a005 = a001 + a002;
a006 = a004 + cur;
a006 += a006;
a004 = a003 - a004;
a003 -= a003;
a004 += a003;
a001 -= 1;
a000 -= a002;
}
a001 -= cur;
a003 += a000;
a000 += a001;
a003 = a006 - a005;
a002 -= a006;
a004 -= a005;
a005 -= a006;
a002 -= 2;
a002 = a006 - a000;
a005 -= a001;
cur += a005;
a005 = a002 + a005;
a006 += a004;
} else {
cur += a005;
a004 = 0 + 3;
a004 = a001 - a000;
a000 = a001 - cur;
a006 -= a000;
}
} else {
a006 -= a006;
}
a002 = a006 + a004;
}
output.collect(prefix, new DoubleWritable(a005));
}
