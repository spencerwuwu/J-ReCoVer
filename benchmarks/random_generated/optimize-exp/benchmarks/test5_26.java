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
a022 = a010 + a014;
a002 += a007;
a014 = a000 + a019;
a011 = a009 - a019;
a024 = a009 + a016;
a019 += a010;
a012 -= a019;
a007 = a014 + a008;
a013 += a007;
a002 += a020;
a003 = a007 - cur;
a003 += a007;
a013 = a021 + a006;
if (a016 <= a006) {
a001 -= cur;
a020 = a005 - a017;
a010 = a012 - a016;
cur += a010;
a023 += a004;
a024 = a009 + cur;
a021 -= a003;
a024 -= a023;
a011 -= a024;
a011 = a004 + a008;
a019 = a008 * -5;
a005 -= a013;
a015 += a020;
a002 = a018 + a020;
a018 -= a000;
a017 += a009;
a014 -= a020;
a011 += a023;
a016 += a006;
a002 += a000;
a013 = a020 - cur;
a006 = a023 - cur;
a018 -= a016;
a018 -= a000;
a005 = a001 - a009;
a012 += a017;
a004 -= a023;
a004 -= a003;
a023 = a003 + a000;
a008 = a020 - a016;
} else {
a021 += a012;
a013 -= a006;
a021 += a014;
a023 -= a009;
a002 += a002;
a016 += a010;
a003 = a000 - a007;
a020 = a022 + a023;
a019 += a024;
a007 -= cur;
a021 = a013 + 3;
a023 -= a014;
a011 = a008 + a005;
a020 = a016 + a003;
a005 -= a014;
a014 = a012 - a000;
a020 = a002 + a006;
a014 = a000 + a001;
cur += a000;
a001 += a021;
a001 -= a019;
a000 = a015 - a001;
a010 -= a023;
a003 = a005 + a024;
a018 = a020 + a013;
a010 += a023;
a007 -= a023;
a022 += a022;
a005 -= a014;
if (a000 != a004) {
if (a020 < a022) {
a009 = a019 + a005;
a017 = cur + a021;
if (a019 < a006) {
a001 += cur;
a014 += a020;
a019 = a020 + a019;
a024 += a001;
a018 = a009 + a012;
a002 += a012;
a021 = a001 - a015;
a013 -= a012;
a003 = a016 + a009;
a023 += a024;
a004 = a019 - a023;
a006 = a019 - cur;
a006 = a012 + a016;
a015 = cur + a000;
cur -= 4;
a006 = a004 - a015;
a022 += a021;
a005 -= a004;
a015 -= a015;
a021 += a018;
a023 = 3 - a008;
a003 -= a024;
} else {
a011 -= a011;
a012 = a018 - a022;
a023 = a020 + a023;
cur += a010;
a024 -= a020;
a021 += a013;
a023 -= a013;
a007 = a009 + a021;
a000 = a019 + a017;
a014 += a005;
a020 = a012 - a021;
a020 = a006 + a010;
a001 = a009 + a000;
a023 = a014 - a009;
a016 = a018 - a004;
if (a016 < a015) {
a018 -= a019;
a000 = a001 * 4;
a024 = a008 - a021;
cur = -4 - a011;
a022 = a020 + a018;
a010 = a004 + a005;
a011 = a022 - a002;
a023 -= a004;
a011 = a015 + a017;
a009 = a021 - a004;
a020 += 1;
a021 = a016 - a015;
a016 -= a022;
} else {
a017 = a007 + a013;
a014 += a019;
a016 -= a013;
a024 = a023 + a006;
a021 += a011;
a021 += a002;
a022 = a020 + a018;
}
a018 += a005;
a007 = a021 - a007;
a016 += a018;
a023 += a009;
a022 += a007;
a021 -= a016;
a019 = a000 + 1;
a014 = a004 + a014;
a003 -= a019;
a013 += a016;
a013 += a012;
a019 = a007 + a002;
a010 = a010 + a004;
a012 -= a012;
a016 += a014;
a017 = a017 - a002;
cur = a012 - a011;
a018 = a014 + a006;
a024 += a007;
a010 = a003 + 3;
a012 -= a006;
a003 -= a019;
a017 = a010 + a019;
a007 = a017 + a001;
a015 = a015 - a016;
a006 -= a000;
a019 -= a007;
a001 = a024 + a013;
a006 += a017;
a012 = a010 * -4;
}
a020 -= cur;
a003 = cur - a016;
a009 += a001;
a016 = cur - cur;
a011 = a012 - a005;
a013 -= a001;
a010 += a011;
a020 = -1 + a020;
a023 += a005;
a016 = a016 + a007;
a011 += a001;
a008 -= -3;
a007 = a022 + a003;
a015 = a016 - a014;
a020 += a011;
a017 += a003;
} else {
a012 -= a017;
a016 = a001 - a021;
a024 += a020;
}
a007 -= a010;
a005 = a005 - a001;
a010 -= a022;
cur = 2 + a005;
a008 = a011 + a013;
a016 += a002;
a016 = a014 - a010;
a019 = a012 - a020;
a006 += a003;
a023 = a005 - a019;
a013 -= a011;
a019 += a009;
a018 = a014 + a023;
a019 = a007 + a014;
a009 -= a006;
a011 += a016;
a009 += a017;
a021 += a018;
} else {
a018 -= a024;
a009 = a001 - a016;
a008 = a016 + a021;
a018 = a023 - cur;
a003 += a019;
cur += a005;
a013 -= a007;
a023 = cur - a019;
a010 = a015 + a024;
a014 = a018 + -1;
a019 = a023 + a010;
a009 -= a011;
a006 += a024;
a003 = a023 - a005;
a024 -= a020;
a010 += a000;
a022 = a000 + a001;
a020 -= a023;
a013 = a009 + a023;
a022 = a001 + a001;
a015 -= a015;
a019 += a010;
a017 = a021 - a009;
a007 -= cur;
a013 = cur + a021;
a018 = a016 + a018;
a007 = a006 - a012;
a024 = a013 + a009;
a019 = a023 - a001;
a023 = a000 - a005;
a018 = a020 - a000;
a007 += a017;
a001 = a021 - a013;
}
a013 -= cur;
cur = a006 + cur;
a013 = a014 - a009;
a018 += a000;
a007 = a005 - a006;
a003 += a010;
a008 = a006 + cur;
a002 -= a007;
a003 += a020;
}
a023 = a014 + a015;
a010 = a023 - a023;
a014 -= a020;
a010 = a022 + a003;
a004 += a001;
a020 -= a007;
a022 = a019 + a013;
a018 -= a020;
a007 -= a009;
a007 = a020 + a024;
}
output.collect(prefix, new IntWritable(a020));
}
