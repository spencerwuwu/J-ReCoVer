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
a021 = a006 + a013;
a007 = a002 - a018;
a012 -= a013;
a022 += a016;
a019 -= a022;
a010 -= a015;
a021 = a015 - a008;
a010 = a016 + a005;
a015 -= a010;
a006 = a024 - a003;
a019 = a000 + a004;
a012 -= a018;
a024 = a018 + a024;
a024 -= a002;
a004 = a009 + a017;
a002 = a004 - a010;
a010 += a001;
if (a002 != a001) {
cur = a012 + a000;
a002 -= a022;
a008 -= a009;
a011 -= -1;
a021 = a004 - a000;
a023 += a015;
a003 = a012 - a023;
a002 = a001 - a005;
cur -= a018;
a000 = a009 + a018;
a014 = a011 + a004;
a015 += a009;
a022 += a008;
a002 = a019 + a007;
} else {
a013 = a020 - a004;
cur += a012;
a000 += a024;
cur = a008 + a023;
a001 = a018 + a000;
a003 = 0 + a018;
a014 = a007 + a008;
a005 = a008 - a011;
cur = cur - a016;
a006 += a017;
a010 = a020 - a002;
a018 = a016 + a022;
a002 += a022;
a006 += a001;
a017 = a021 + 0;
a019 -= a014;
a011 -= a005;
a012 += a018;
a018 += 0;
a009 -= a009;
a004 = a015 - a011;
a002 = a012 - a017;
a021 = a020 - a017;
a023 += a019;
a016 = a008 - a021;
}
a012 += -1;
a013 = a008 + 1;
if (a024 >= cur) {
a001 -= a008;
a024 = a019 + a005;
a009 = a018 - a019;
a023 += a019;
a023 -= a020;
a002 = a001 - a014;
a003 = a008 - a000;
a010 += a015;
a016 = a019 - a018;
a015 -= cur;
a014 += a004;
cur = a009 + a022;
a023 = a015 - a008;
a020 += a000;
a006 = a011 - a020;
a022 += a014;
a013 -= a000;
a018 = a011 - a005;
a014 = -4 - a012;
a020 = a011 - a021;
a010 += a009;
a014 = a020 - a009;
} else {
a005 -= cur;
a017 = a014 + a002;
a021 -= a013;
a002 = a024 + a021;
a018 = 1 - a005;
a013 -= a018;
a006 -= a024;
a022 = a023 - a008;
a000 += a007;
cur += a007;
a008 = a010 + a004;
a015 = a015 - a010;
a008 += a016;
a010 += a015;
a013 = a008 - a020;
}
cur -= cur;
a013 = a000 - a018;
a004 += a015;
a004 += a013;
a024 -= a018;
a004 -= a016;
a016 = a014 + 2;
a015 = a019 - a020;
a022 = a013 - a000;
a005 = a001 + a010;
a006 = a019 + a019;
a017 = a000 - a003;
a018 = a015 - a007;
a005 -= a012;
a010 = a006 - a022;
a008 = a014 + a012;
a014 = a019 + a017;
a002 -= a020;
a013 = a020 - a004;
a009 = a016 + a007;
a018 = a004 - a014;
a013 += a016;
a007 = a002 + a018;
a020 -= a010;
a006 = a014 - a005;
cur = a000 - a020;
a018 -= a001;
if (a023 > a022) {
a023 = a000 - a012;
a001 -= 3;
a018 -= a005;
a001 = a023 + a019;
a023 += a012;
} else {
a016 = a002 - a006;
a006 = a000 + a012;
a017 += cur;
a021 -= a003;
a015 = a020 + a005;
a003 -= a002;
a003 = a020 - a023;
a009 = a009 + a002;
a006 -= a016;
a004 = a014 - a015;
a016 = a012 + a001;
a000 -= a013;
a022 = a020 - a022;
a011 += -3;
a020 -= a010;
a023 = a024 + 0;
a004 = a001 + a010;
a021 = -5 - a000;
a009 = a020 - a021;
cur -= a017;
a006 -= cur;
a024 = a011 + a020;
a006 = a013 + a012;
a014 = cur - a010;
a018 -= a006;
a000 = a005 - a007;
if (a010 > a024) {
a016 -= a003;
a011 = a012 - a018;
a003 = a008 - a005;
a017 += a023;
a002 -= a004;
if (a012 >= a009) {
a001 = a006 - a010;
a012 = a004 + a003;
a019 = a005 + a009;
a017 -= cur;
a021 += a019;
a007 = a012 + a017;
a011 = a009 + a006;
a003 = a024 + a016;
a021 -= a013;
a008 = a002 + a015;
a001 += a002;
a019 -= a024;
a003 -= a014;
} else {
a024 = a003 + a003;
a019 -= a011;
a022 += a005;
a024 += a010;
cur -= a004;
a008 = a021 + a005;
a005 = a009 - a005;
a019 = a000 + a020;
a016 -= a007;
a020 += a015;
a010 = a022 + a002;
a014 -= a008;
a023 += a016;
a005 = a013 - a015;
a022 = a020 - a012;
a014 = a016 - a011;
a024 += a006;
a022 = a017 - a011;
a019 = a016 - a004;
a005 -= cur;
a000 += a011;
a009 = a019 - a008;
a001 = a010 - a005;
a017 -= a006;
a024 += a011;
a005 -= a012;
a015 -= a015;
a003 -= a010;
a023 += a003;
a013 += a007;
}
a020 = a012 - a012;
a000 -= a013;
a024 += a002;
a018 += a020;
a015 -= a004;
a008 = a005 - cur;
a014 = 3 - a015;
a016 -= cur;
a019 = a015 - a013;
cur -= a003;
a015 -= a002;
a002 = a010 + a018;
a009 = a011 - a022;
a000 += a021;
a008 = a009 - a006;
a015 = a006 - a017;
a011 += a019;
a013 -= a008;
a020 -= a004;
a019 = a016 - a007;
a014 += a010;
a020 -= a020;
a017 += a006;
a023 = cur - a006;
} else {
a020 -= a017;
a015 = a000 + a007;
a005 = a001 - a009;
a020 += a000;
a005 += a019;
a023 = a002 + a000;
cur -= a020;
a017 = a018 + a004;
a002 = a020 + -3;
a024 = a006 + a005;
cur += a019;
}
a004 = a018 - a018;
a010 -= a001;
a018 = a007 - a023;
a022 -= a023;
a024 = a004 - a013;
a020 += a021;
a013 += cur;
a020 += a001;
a002 = a001 - a016;
a002 = a012 - a016;
a012 += a014;
a006 -= a015;
}
a019 = a002 + cur;
a017 = a006 + a001;
}
output.collect(prefix, new IntWritable(a002));
}
