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
a013 = cur - a011;
cur = a001 + a010;
a010 = a007 + a012;
a024 = a021 + a005;
a024 = a005 + a007;
a019 = a022 - a013;
a004 = 2 - a004;
a015 = a011 - a021;
a011 += a022;
a021 = a022 + a007;
a013 = a019 + a012;
a023 += a024;
a000 += a011;
a019 -= a017;
a006 -= a005;
a005 = a019 - a007;
a002 -= a014;
a013 += 1;
a024 = a024 - a023;
cur -= a022;
a013 = a004 - a007;
a002 = a011 + a011;
a017 = 4 + a016;
a005 -= a008;
if (a011 >= a004) {
a000 = a015 + a009;
a010 -= a020;
a016 = a004 + a005;
a001 += a013;
a009 += a018;
a009 += a010;
a004 += a002;
a000 = a011 + a015;
a001 = a019 + a011;
a006 = a013 + a001;
a013 = a012 - a015;
a007 = a020 + a017;
a008 = a022 - a001;
a011 -= a009;
a014 += a008;
a008 = a010 - a007;
a016 = a020 - cur;
a023 += a017;
a003 += a018;
a002 = a014 - a021;
a012 -= cur;
a002 = a013 - a020;
a010 = a018 + a016;
a014 = a018 - a018;
a004 += a011;
a014 += a016;
a005 += a001;
a007 = a003 + a007;
a021 = a010 + a020;
a007 = a010 - a023;
} else {
a017 -= a006;
a015 = a003 - a010;
a021 = a007 - a011;
a001 -= a010;
cur = a000 - a006;
a003 = a020 + a002;
a020 = a003 - a001;
a019 = a004 + a002;
a018 -= a024;
a017 -= a011;
if (a017 != a021) {
a016 = a013 + a007;
a001 -= a008;
a017 -= a007;
a016 -= a013;
a007 += a007;
a023 -= a013;
a002 += a013;
a019 = a021 + a023;
cur += a004;
a006 -= a012;
} else {
a024 = a011 - a017;
a008 -= a011;
a023 = a021 - a020;
a020 -= a021;
a008 -= 2;
a000 -= a015;
a009 -= a003;
a007 -= a006;
a023 -= cur;
a000 -= a022;
a024 += a008;
a007 = a000 - a011;
a002 = a008 + a001;
a006 -= a018;
a016 = a016 - a004;
a013 = a011 + cur;
a011 = a019 - a024;
a009 = a003 + a019;
a003 = a009 - a016;
a001 = a023 - a003;
a004 = cur + a019;
a019 = a014 + a012;
a014 -= a009;
}
a011 = a023 + 3;
cur = a016 - a020;
a015 -= a007;
a019 = a012 - a018;
a016 += a002;
a020 += a001;
a017 = a011 - a002;
a015 = a019 + a012;
a019 += a022;
a011 = a024 - a001;
a001 += a014;
a012 += a009;
a014 += -1;
a008 = a013 + a009;
a006 = a011 + a006;
a004 = a022 + a021;
a011 = a018 + cur;
}
a000 -= a001;
a001 = a024 + a020;
a011 = a023 + a002;
a007 += a009;
if (cur <= a015) {
a003 = a017 + cur;
a020 = a008 - a002;
a019 = a020 + a006;
a003 = a008 - a022;
a014 -= cur;
a005 += a004;
a010 -= a024;
a023 += a002;
a000 -= cur;
a008 -= a006;
a001 = a021 + a003;
a014 = a007 - a024;
a009 = a014 - a007;
a014 += a020;
cur = a011 - a004;
a023 -= a015;
a001 -= a008;
a011 = a001 + a000;
a006 -= a010;
a009 = a007 - a006;
} else {
a015 = a003 + a004;
a013 = a014 - a010;
a016 = a011 + a019;
a020 -= a017;
a006 = a014 - a023;
a023 = a012 + a012;
cur = a009 - a013;
a002 += a003;
a003 = a006 + a022;
a006 = a002 + a016;
a021 += a015;
a016 = a012 + a018;
a000 = a015 + a005;
a009 = a014 - -3;
a008 = cur - a011;
a010 = a018 - a022;
cur -= a002;
a014 = a004 + a003;
a012 -= a023;
a017 = a022 - a018;
a016 = a021 - a015;
a007 += a003;
a019 += a023;
}
a009 -= a001;
a017 = a022 + a020;
a011 += a013;
a017 = 3 + a002;
a013 += a014;
a017 += a018;
a014 = a018 - a021;
a017 -= a023;
a016 -= a002;
a004 = a006 - a011;
a022 += a011;
a016 = a009 + a024;
if (a010 >= a012) {
a021 = a020 - a023;
a000 += a001;
a010 -= a001;
a009 = a019 - -3;
a007 += a019;
a008 += a015;
} else {
a002 += a006;
a013 += -3;
cur += a021;
a011 = a013 - a008;
a021 = a011 * -1;
if (a016 < a021) {
a020 += a014;
a006 += a024;
a009 = a013 - a007;
a012 = a004 + a000;
a018 = a003 + a002;
a005 = a004 - a006;
a015 = a005 + a008;
a004 = a008 + a007;
cur -= a018;
a011 += -1;
a011 = a018 + a021;
a023 -= a013;
a023 += a009;
a014 += a002;
a024 += -3;
a009 -= -4;
a021 = a002 - -3;
cur -= a016;
a020 = a014 - a019;
a002 += a013;
a008 -= a003;
} else {
a013 += a021;
a015 = a006 + a016;
a019 = a021 - a005;
a024 += a004;
a020 += a018;
a024 = -4 + a021;
a000 = a005 + a000;
a023 = cur - a024;
a018 = a006 + a022;
a009 = a018 - cur;
a013 -= cur;
a021 -= a023;
a022 += a022;
a013 -= a009;
a012 = a021 - a001;
a020 = a014 - a020;
a001 = a013 - a012;
a012 = -3 + a003;
}
a004 -= a000;
a014 -= a011;
a009 -= a007;
a024 += cur;
a015 = a004 + a014;
a011 -= a018;
a006 += a013;
a021 += a004;
a007 += a024;
a006 -= a008;
a021 = a001 + a010;
}
a000 = a015 - a006;
a015 += a021;
a016 += a022;
a008 = a014 - a020;
a020 = a012 - a004;
a011 = a000 - a014;
a009 += a013;
a001 += 1;
a010 = a011 - a018;
a004 -= a002;
a016 = a004 * -4;
cur -= a016;
a011 = a012 - a011;
a018 = a008 + a022;
a015 = a006 - cur;
a015 += a004;
}
output.collect(prefix, new IntWritable(cur));
}
