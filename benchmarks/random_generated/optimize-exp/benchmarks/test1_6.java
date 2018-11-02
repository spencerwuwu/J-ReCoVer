// Note: only +, - operations
// Parameters:
//   Variables:   5
//   Baselines:   50
//   If-Branches: 1

public void reduce(Text prefix, Iterator<IntWritable> iter,
         OutputCollector<Text, IntWritable> output, Reporter reporter) throws IOException {
int a000_ = 0;
int a001_ = 0;
int a002_ = 0;
int a003_ = 0;
int a004_ = 0;
int cur_ = 0;

while (iter.hasNext()) {
cur_ = iter.next().get();
a003_ = a004_ - a003_;
cur_ = a003_ + a002_;
cur_ = a000_ * -1;
a000_ = -5 - a002_;
cur_ = cur_ + a003_;
a002_ = 4 + a003_;
a003_ = a003_ - a003_;
a002_ = a000_ - a001_;
cur_ = a004_ + -4;
a002_ = a001_ - a004_;
a003_ = a003_ + a004_;
a000_ = 4 + a001_;
a002_ = a001_ - a003_;
a003_ = a003_ + cur_;
a000_ = a001_ + a001_;
a003_ = -4 - a000_;
a004_ = a000_ - 0;
a001_ = a003_ - a003_;
a000_ = a001_ - a000_;
cur_ = -2 - a002_;
a004_ = cur_ - a000_;
a001_ = a001_ - cur_;
a004_ = -2 - 4;
a001_ = a004_ + 2;
a001_ = a001_ - a001_;
a002_ = a004_ + a001_;
cur_ = a004_ - a000_;
if (cur_ == cur_) {
a003_ = cur_ + a002_;
cur_ = a001_ - -2;
a003_ = 1 + cur_;
a001_ = a000_ - a004_;
a002_ = a002_ - a000_;
a001_ = a001_ - a001_;
a000_ = cur_ + a002_;
a004_ = a003_ + a003_;
a002_ = a002_ - a003_;
a004_ = cur_ + cur_;
a004_ = a004_ + a001_;
a003_ = a004_ + a002_;
cur_ = -5 + a001_;
a004_ = cur_ + -1;
} else {
a004_ = -2 - a004_;
a000_ = a000_ + a001_;
a000_ = a004_ - a003_;
a000_ = a002_ + cur_;
a001_ = a000_ - -4;
}
a001_ = cur_ + cur_;
a001_ = a000_ + cur_;
a004_ = a004_ - a001_;
a001_ = -2 - 4;
}
output.collect(prefix, new IntWritable(a001_));
}
