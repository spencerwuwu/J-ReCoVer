// Note: only +, - operations
// Parameters:
//   Variables:   15
//   Baselines:   150
//   If-Branches: 3

public void reduce(Text prefix, Iterator<IntWritable> iter,
         OutputCollector<Text, IntWritable> output, Reporter reporter) throws IOException {
int a000_ = 0;
int a001_ = 0;
int a002_ = 0;
int a003_ = 0;
int a004_ = 0;
int a005_ = 0;
int a006_ = 0;
int a007_ = 0;
int a008_ = 0;
int a009_ = 0;
int a010_ = 0;
int a011_ = 0;
int a012_ = 0;
int a013_ = 0;
int a014_ = 0;
int cur_ = 0;

while (iter.hasNext()) {
cur_ = iter.next().get();
a003_ = a013_ + 0;
a010_ = a009_ - a003_;
a005_ = a004_ + a013_;
if (a014_ >= a005_) {
a007_ = a014_ - a001_;
cur_ = a009_ + a002_;
a014_ = a000_ - a007_;
a014_ = a003_ + a005_;
a002_ = a010_ - a001_;
a005_ = cur_ + a003_;
a000_ = cur_ + a009_;
a008_ = a003_ - a014_;
a011_ = cur_ + a010_;
a014_ = a006_ + a002_;
a008_ = a006_ + a010_;
a006_ = a012_ - a009_;
a005_ = a001_ + a004_;
a005_ = a007_ + a004_;
a005_ = a013_ - a008_;
a014_ = a008_ + a009_;
a007_ = a005_ + a004_;
if (a007_ > cur_) {
a008_ = a001_ - a011_;
a001_ = a009_ - a011_;
a014_ = a004_ - a007_;
a000_ = a011_ - a003_;
a013_ = a003_ - a008_;
a001_ = 4 + a001_;
a001_ = a007_ + a011_;
a000_ = a002_ + a012_;
a013_ = a011_ + a008_;
a001_ = a009_ + a006_;
a012_ = a005_ - a011_;
a000_ = a004_ - cur_;
a012_ = a006_ - a010_;
a013_ = a009_ + a002_;
a008_ = -2 + a002_;
a011_ = a005_ - a009_;
a004_ = a006_ + a011_;
a005_ = a003_ + a000_;
a005_ = a014_ + cur_;
a004_ = cur_ + a007_;
cur_ = a012_ + a006_;
a004_ = a011_ - a005_;
a007_ = a003_ - a000_;
a009_ = a007_ - a013_;
a009_ = a007_ - a010_;
a014_ = a008_ + a003_;
a003_ = cur_ + a000_;
a005_ = 1 + a005_;
a013_ = a004_ - cur_;
} else {
a000_ = cur_ + a001_;
cur_ = a007_ - a005_;
a004_ = a008_ + a007_;
a008_ = a007_ - a004_;
a013_ = a003_ + a003_;
a008_ = a002_ + 0;
cur_ = a000_ + a005_;
a000_ = a005_ + a000_;
a008_ = cur_ - a007_;
a007_ = a012_ + a006_;
a002_ = a012_ - cur_;
a009_ = a003_ + a006_;
a013_ = a004_ - a013_;
if (a003_ >= 1) {
a013_ = a008_ - a003_;
a001_ = a010_ - a012_;
a000_ = a006_ + a000_;
a011_ = 0 + a013_;
a001_ = a007_ - a012_;
a011_ = a013_ + a000_;
a008_ = a004_ - a014_;
} else {
a001_ = a011_ - a013_;
a010_ = a013_ - a009_;
a009_ = a007_ - a011_;
a001_ = cur_ + a001_;
a013_ = a003_ + -5;
a001_ = a011_ - a008_;
a012_ = a002_ - a009_;
a004_ = a008_ - a003_;
a003_ = a008_ + a002_;
a001_ = a008_ + a004_;
a002_ = a014_ + a011_;
a001_ = a006_ - a007_;
a013_ = a001_ + a007_;
a002_ = a013_ - a013_;
a006_ = a007_ - a004_;
a004_ = a010_ + a009_;
a004_ = a012_ - a001_;
a008_ = a003_ + -3;
a002_ = a000_ + a002_;
a001_ = a014_ + a001_;
a009_ = a013_ + a007_;
}
a007_ = a004_ - a006_;
a004_ = a002_ - a000_;
a001_ = a002_ + a011_;
a008_ = a007_ + a002_;
a011_ = a002_ + a005_;
a007_ = a004_ + a005_;
a010_ = a003_ - a002_;
a003_ = a009_ - a013_;
a012_ = -3 + a003_;
a006_ = cur_ - a004_;
cur_ = a012_ - a002_;
a011_ = a013_ - 4;
a006_ = a014_ + a008_;
cur_ = a011_ - a001_;
a004_ = a009_ + a005_;
a004_ = cur_ - a007_;
a010_ = a011_ - a014_;
a003_ = a006_ - a005_;
a003_ = a011_ - a006_;
a002_ = a013_ + a001_;
a010_ = a006_ + a012_;
a014_ = cur_ - a013_;
a014_ = a005_ - 1;
a010_ = a013_ - a001_;
a009_ = a014_ + a011_;
a001_ = a008_ + a006_;
a014_ = a011_ + 2;
a004_ = a014_ + a002_;
a000_ = a013_ - a010_;
}
a005_ = a010_ + a010_;
a001_ = cur_ + a000_;
} else {
a004_ = a010_ - -5;
a005_ = a003_ - a001_;
a001_ = a002_ + a010_;
a014_ = a002_ + a000_;
a011_ = a007_ - a003_;
a009_ = a013_ - a005_;
a002_ = a005_ - a006_;
a000_ = a010_ + a004_;
a008_ = a008_ - a008_;
a008_ = a006_ + a007_;
a001_ = a011_ - a002_;
cur_ = a004_ + a005_;
a011_ = a014_ + a013_;
}
a003_ = cur_ + a004_;
a013_ = a006_ + a012_;
a004_ = cur_ + a010_;
a010_ = cur_ - cur_;
a005_ = a005_ + a014_;
a012_ = a008_ + a005_;
a012_ = a007_ + a006_;
a005_ = a004_ - a014_;
a012_ = cur_ + a004_;
a014_ = a014_ - a012_;
a009_ = a014_ - a009_;
a011_ = a006_ + a010_;
a005_ = a005_ - a014_;
a009_ = a001_ - a012_;
a003_ = a001_ + a005_;
a014_ = a012_ - 4;
}
output.collect(prefix, new IntWritable(a007_));
}
