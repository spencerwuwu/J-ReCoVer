// Note: only +, - operations
// Parameters:
//   Variables:   10
//   Baselines:   100
//   If-Branches: 2

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
int cur_ = 0;

while (iter.hasNext()) {
cur_ = iter.next().get();
a000_ = a004_ + a003_;
a000_ = a004_ + cur_;
a007_ = a004_ - a001_;
cur_ = cur_ + a005_;
a002_ = -2 + a000_;
a008_ = a002_ - a004_;
if (a000_ != a005_) {
a004_ = a005_ + a006_;
a007_ = a003_ + a008_;
a002_ = a008_ + a000_;
a002_ = a003_ + a000_;
a001_ = a008_ + a003_;
cur_ = a007_ - a007_;
a002_ = 4 + a004_;
a008_ = a000_ + a000_;
a008_ = a005_ + a008_;
cur_ = a001_ - cur_;
a001_ = a009_ - a001_;
} else {
a005_ = a009_ * -3;
cur_ = a009_ + a004_;
a003_ = a001_ - a004_;
a005_ = a002_ + a008_;
if (a008_ < a002_) {
a003_ = a006_ + a002_;
a002_ = a007_ - a006_;
a005_ = a005_ + a004_;
a002_ = a003_ + a008_;
a001_ = a008_ + a002_;
a004_ = cur_ + a001_;
cur_ = a004_ - a000_;
a004_ = a001_ + a009_;
a003_ = a003_ - a002_;
a001_ = cur_ + a001_;
a000_ = a008_ - a000_;
cur_ = a005_ + a003_;
a004_ = -3 - a005_;
a009_ = a003_ - cur_;
a009_ = 0 - a001_;
a008_ = a001_ - cur_;
a007_ = a007_ - a006_;
a003_ = a007_ + a001_;
a008_ = a008_ - a005_;
cur_ = a009_ - a000_;
a007_ = a008_ - a006_;
a008_ = -3 + a007_;
a009_ = a009_ + -5;
a005_ = a000_ - a001_;
a007_ = cur_ + a004_;
cur_ = a003_ - a004_;
} else {
a003_ = a009_ + a003_;
a008_ = a003_ - a008_;
a005_ = -2 + a008_;
cur_ = cur_ - a004_;
a008_ = a009_ + a000_;
a009_ = a005_ - a005_;
a002_ = a009_ - a008_;
a004_ = a005_ - a003_;
a000_ = a003_ - a001_;
a006_ = a003_ + a004_;
a007_ = a003_ + a003_;
a005_ = a001_ - cur_;
cur_ = cur_ - cur_;
a002_ = a006_ - cur_;
a008_ = a009_ - a002_;
cur_ = a004_ - -2;
a004_ = a000_ - a000_;
a002_ = a005_ - a007_;
a009_ = a006_ + a005_;
a002_ = 3 + a002_;
cur_ = a004_ - a008_;
a008_ = a002_ - a003_;
a003_ = a006_ + 1;
a002_ = a002_ - cur_;
a001_ = a006_ + cur_;
a003_ = a003_ + a002_;
a004_ = a000_ + a000_;
}
a008_ = a007_ - cur_;
cur_ = a001_ - a001_;
a006_ = a002_ - cur_;
a005_ = cur_ - a006_;
a002_ = a004_ + a001_;
a000_ = a003_ + a000_;
a007_ = a009_ - a003_;
a000_ = 0 - a002_;
a003_ = a000_ - a008_;
a001_ = cur_ - a005_;
a008_ = a009_ + a000_;
a001_ = a000_ - a009_;
a000_ = -2 - a007_;
a001_ = a009_ + a002_;
a000_ = a008_ - a002_;
a009_ = a001_ + a005_;
a003_ = a001_ - a005_;
}
a008_ = a006_ + a000_;
a003_ = a006_ - a009_;
a001_ = a006_ - a003_;
a008_ = a002_ - a002_;
a001_ = cur_ - a002_;
a008_ = -1 - a000_;
a009_ = a003_ + a001_;
cur_ = a004_ + cur_;
cur_ = a004_ - a001_;
}
output.collect(prefix, new IntWritable(a005_));
}
