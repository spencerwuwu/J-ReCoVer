// Note: only +, - operations
// Parameters:
//   Variables:   25
//   Baselines:   250
//   If-Branches: 5

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
int a020 = 0;
int a021 = 0;
int a022 = 0;
int a023 = 0;
int a024 = 0;
int cur = 0;

while (iter.hasNext()) {
cur = iter.next().get();
a010 = a013 + cur;
a007 -= a021;
a020 += a003;
a008 = a011 - a015;
a007 += a012;
a003 += a017;
a014 = a017 - a024;
a018 += a010;
if (a002 > a023) {
a003 -= a005;
a021 = a006 - a019;
a017 -= a011;
a023 += a003;
a024 = a023 + a004;
a003 += a001;
cur = a023 + a012;
a015 += a023;
a012 = a016 + a008;
a010 = a021 - a002;
a024 = a010 + a004;
a001 = a019 - a005;
a017 = a003 - a007;
a015 += a024;
a018 += a001;
a021 += a014;
a008 -= a013;
a005 -= a012;
a018 = a003 + a006;
a006 += a005;
a009 = a014 - a009;
} else {
a024 = a024 - a005;
if (a022 >= a015) {
a003 -= a002;
a017 = a003 - a014;
a006 += a001;
a005 += a011;
a005 = a014 * -5;
a012 = a010 - a019;
a016 = a001 - a019;
a005 = a018 + a016;
a012 = a002 - a009;
a000 = a012 + a020;
a014 = a015 + a008;
a002 = a000 - a008;
a011 = a021 - a009;
a011 += a024;
a012 = a000 - a017;
cur = a021 - a010;
a020 -= a003;
} else {
a018 -= a024;
a024 -= a002;
a000 += a012;
a018 += a006;
a003 = a007 - a011;
a005 += a016;
a001 += a012;
a018 = a000 + a005;
a009 = a014 + a001;
cur += -3;
a005 -= a021;
a003 += a005;
a012 = cur + a015;
a010 -= a020;
cur += a003;
a015 -= -2;
a007 -= a008;
a004 += -5;
a004 += a006;
a008 -= a016;
a007 = a003 + a008;
a017 -= a001;
a023 = a014 + a003;
a018 += a006;
a008 = a018 - a000;
a024 += a021;
a003 += a007;
a019 = a016 + a006;
}
a003 += a005;
a003 = a004 + a015;
a012 -= a016;
a015 -= a010;
a011 += a018;
cur -= a005;
a011 += a021;
if (a024 < a014) {
a004 = a001 - a024;
a001 -= a017;
a020 = a001 - a024;
a017 = a004 - 2;
a018 += a009;
a009 = a004 + a007;
if (a011 < a019) {
a012 = a001 + a023;
a000 = a008 + a000;
a004 = a001 + a009;
a017 -= a002;
a009 = a007 + cur;
} else {
a004 -= a019;
a016 -= a015;
a015 = cur - a008;
a018 = a001 * 1;
a007 = a021 + a003;
a008 = a008 + a002;
a004 = a020 + a005;
a016 = a009 - 3;
}
a013 = a019 + a016;
cur -= a021;
a023 = a005 + a009;
a008 += a019;
a013 += a007;
a019 = a022 - a018;
a021 = a023 - a007;
cur -= a023;
a014 += a002;
if (a015 != a017) {
a005 = a010 + a017;
a018 += cur;
a004 = a016 - a019;
a001 -= a001;
a023 -= a004;
a014 = -2 - a014;
a024 = cur + a019;
a010 = a003 - a005;
a020 = a001 - a021;
a002 = a002 - a021;
a018 += a016;
a002 = a023 - a000;
a008 = a011 - a021;
a002 += a001;
a020 = a011 - a016;
a022 += a018;
a007 -= a020;
a012 += a003;
a012 += a009;
a004 += a019;
a006 = a004 - a015;
a019 -= a024;
a001 = a012 + a004;
a005 += a007;
a013 = a001 - a016;
a019 = a008 + a004;
a016 += a003;
a020 = a002 - cur;
a023 += a018;
} else {
cur = a020 + a000;
a019 += a008;
a012 += a006;
a007 += a001;
a006 += a015;
}
a012 = a006 + a023;
a012 = a011 + a021;
a010 += a012;
a006 = a014 + cur;
a003 += a017;
} else {
a010 = a018 + a002;
a009 = a004 - a008;
cur = a020 * 3;
a018 = a006 - a016;
a022 = a000 + a010;
a015 = a007 + a001;
a018 += a004;
a010 = a002 + a013;
a002 = a010 - a021;
a023 = a013 + a013;
a018 = a022 - a008;
a020 = a003 - a018;
cur = a015 + a012;
a015 = a012 + a024;
a018 = a024 + a013;
a023 = a009 + a015;
a004 = a005 + a022;
a004 = a008 + a009;
a021 = a023 + a013;
}
cur = a003 + a002;
a004 = a004 - a003;
a008 += a012;
a020 -= a004;
a019 = a021 + a022;
a006 = a005 + a001;
a011 = 1 + a012;
a009 -= cur;
a002 += a009;
a024 = a017 - a023;
a007 += a007;
a004 -= a003;
a018 = a015 - a019;
a017 = a024 + a020;
a003 = a024 + a014;
a009 = a009 + a011;
a012 = a013 + a020;
a010 = a021 - a023;
a009 += a021;
a005 -= a021;
a019 = a018 + a010;
a014 += a007;
a008 += a015;
a002 -= a020;
a002 = a017 - a020;
a023 -= a018;
a013 = a000 + 4;
a007 = a022 - a014;
a018 -= a019;
cur += a010;
a020 -= a005;
a012 = a016 - a006;
}
a010 = a014 + a013;
a009 += a013;
a012 = a022 - a019;
a007 = a022 - a008;
a015 = a009 + a005;
a021 -= a023;
a005 += a001;
a002 = a011 + a024;
a019 += a019;
a021 = a010 - a017;
a014 = a016 + a018;
a009 = a007 * -3;
a002 -= a010;
a019 += cur;
a021 -= a010;
a002 = a019 + a015;
a000 = a024 - a023;
a008 -= a017;
a004 -= a021;
a015 = a003 * 3;
a001 = a006 - a008;
cur = a008 - a014;
a021 = a010 + a018;
a018 = 1 + a002;
a012 = a020 + a001;
a011 = a022 * 3;
a012 -= a013;
a007 -= a023;
a012 -= a001;
a013 = a019 - a009;
a017 -= a000;
a013 += a005;
a005 = a024 + a010;
a015 -= a007;
a005 += a014;
a006 = a012 + a024;
a013 += a016;
a020 = a008 - a022;
a021 = a006 + a006;
a008 = a023 + a022;
a000 += a005;
a010 -= a009;
a024 = a015 + a012;
a005 = a002 - a008;
a017 -= a015;
a011 -= a008;
a005 += -2;
a008 = a021 * -3;
a007 = a001 + 1;
a016 += a023;
}
output.collect(prefix, new IntWritable(a023));
}
