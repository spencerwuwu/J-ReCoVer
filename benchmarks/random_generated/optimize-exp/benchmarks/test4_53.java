// Note: only +, - operations
// Parameters:
//   Variables:   20
//   Baselines:   200
//   If-Branches: 4

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
int a015 = 0;
int a016 = 0;
int a017 = 0;
int a018 = 0;
int a019 = 0;
int cur = 0;

while (iter.hasNext()) {
cur = iter.next().get();
if (a019 > a004) {
a011 -= a002;
a016 = a010 + a000;
a004 += a016;
a015 = a000 - a005;
a019 -= a005;
a007 -= a018;
a014 -= a003;
a001 = a007 + 3;
a009 += a012;
a001 += a004;
a005 = a019 - a013;
a014 += a003;
a011 = a007 + a007;
a011 = a019 - cur;
a007 -= a011;
a015 += a008;
a009 = a014 + a017;
a010 += a015;
a004 -= 4;
a019 += a008;
} else {
a017 -= cur;
a012 = a000 - a004;
a008 += a003;
a019 = -2 - a019;
a001 = a015 + a012;
a018 = a007 - a010;
a012 -= a014;
a013 -= a002;
a007 += a006;
a014 -= a001;
cur = a009 - a000;
a002 = a009 - a015;
a014 = a002 + a008;
}
a018 = a006 - a000;
a002 = a013 + a015;
a005 -= a006;
a010 = a015 * 3;
a016 += a002;
a003 -= a016;
a013 += a010;
a009 += a008;
a009 = a019 - a003;
a007 = cur + cur;
a006 = a001 - a007;
a017 -= a015;
a001 = a016 + a013;
a016 = -5 + a002;
a001 += a017;
a008 -= a019;
a016 -= a003;
a000 = -3 - a005;
a018 = a014 + a006;
a006 -= a013;
a017 -= a001;
if (a008 == a004) {
a007 -= a009;
a008 = a004 + cur;
a017 -= a014;
a019 -= a016;
a019 += a005;
a001 += a016;
a006 = a019 + a004;
a009 = a003 + a018;
a001 = a011 + a019;
a000 = a002 + a001;
a010 = a014 - a015;
a003 += a005;
a002 += a006;
a006 = a016 + a013;
a000 = a015 - a016;
a004 -= a011;
a019 -= a012;
a008 = a007 - a007;
} else {
a000 -= a004;
a005 = a018 - a012;
a000 = a002 - a012;
a007 -= a019;
a017 -= a019;
cur += a010;
a011 -= a019;
cur = cur - a017;
a009 = a006 + a000;
a005 -= a019;
a019 += a007;
if (a014 >= a013) {
a019 += a014;
a007 = a013 + a008;
a011 = a018 + a016;
a003 = a019 + a013;
cur += a010;
a008 += a011;
a013 = a001 + a019;
a004 -= cur;
a009 = a010 + a010;
a016 -= a000;
a017 += a000;
a019 = a006 + a016;
a003 = a009 - a000;
a015 = a015 + a015;
a004 += a010;
a010 = a002 - a006;
a015 = a004 + 3;
a012 += a015;
a002 -= cur;
if (a013 == a018) {
a008 = a004 + a001;
a004 -= a013;
a016 += -2;
a010 -= a016;
a005 = a005 - a018;
a016 = a001 - a009;
a013 = a006 + a009;
a009 -= a005;
a015 += a011;
a004 += a007;
a010 -= a006;
a018 -= -5;
a008 = a014 - a018;
a002 = a007 + a001;
} else {
a015 += a003;
a004 -= a017;
a000 = a008 - a011;
a000 += a006;
a017 -= a014;
a007 += -5;
a017 = a007 + a013;
a010 -= a012;
a006 = a000 - a011;
a017 += a014;
a005 += a017;
a012 = a008 - a011;
a012 = 3 + a000;
a018 -= a015;
a002 -= -2;
a003 = a012 + a019;
a002 += a016;
a017 = a013 - a001;
a002 += -5;
a002 = a016 + a018;
a001 = a008 - a009;
a019 += a002;
a010 = a000 - a010;
a003 -= a010;
a014 = a015 - a010;
a005 = a010 - a016;
a013 += a003;
a007 -= a016;
a016 += a002;
a009 += a009;
}
a011 = a000 - a016;
a019 -= a014;
a012 = a016 + a012;
a009 += a007;
a016 = a014 - a014;
a005 = a006 + a017;
a018 = a000 - a014;
a018 = a005 + a009;
a001 = a013 - a000;
a018 = 2 - a004;
a018 = a012 + a003;
a015 -= a016;
a015 = a019 - a002;
a011 -= a000;
a016 -= a002;
a002 += a011;
a003 += a016;
} else {
a008 -= a008;
a014 = a008 * -5;
cur += a016;
cur += a005;
a013 = a013 - a002;
a001 -= a011;
a007 = a014 + a013;
a006 += a013;
a016 -= cur;
a018 = a007 + a004;
}
a006 -= a003;
a008 = a012 + a012;
a010 += a006;
a010 += a015;
a003 -= a017;
a012 -= a016;
a000 = a013 + a018;
a017 += a006;
a006 = a018 + cur;
a005 += a011;
a009 = a015 - a016;
a007 -= a004;
a008 = a008 - a011;
a010 = a002 - -5;
cur -= a016;
a011 = a005 + a010;
a006 -= a018;
a002 += cur;
a015 -= a007;
}
a005 = a006 - a004;
a019 = a001 + a015;
a018 = a007 + a005;
cur += a000;
a009 = a003 + a009;
a009 = a016 + a002;
a001 += a003;
a014 = a015 + a005;
}
output.collect(prefix, new IntWritable(a019));
}
