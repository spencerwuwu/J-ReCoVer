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
a000_ = a004_ + a000_;
cur_ = a004_ - a001_;
a004_ = a000_ + cur_;
a002_ = -3 + a002_;
a004_ = a000_ + a000_;
a004_ = a001_ - a000_;
a003_ = a001_ + 1;
a003_ = a002_ + a003_;
a000_ = a004_ + a002_;
cur_ = 2 + a002_;
a003_ = a002_ + a003_;
a000_ = a002_ + a002_;
a004_ = cur_ + a002_;
a001_ = cur_ + 0;
cur_ = a004_ + a003_;
a000_ = a000_ + 4;
a003_ = a001_ + a002_;
if (a003_ == a002_) {
a003_ = a003_ - a000_;
a003_ = a002_ - a002_;
a003_ = a002_ - a002_;
a002_ = 1 + a001_;
a001_ = a004_ - a003_;
a004_ = a003_ - a000_;
a003_ = a002_ - a003_;
a002_ = a003_ - a000_;
a004_ = a001_ + a003_;
a004_ = a004_ - a004_;
a002_ = a000_ + a002_;
a004_ = a003_ - -4;
a003_ = cur_ - a004_;
a000_ = a002_ + a003_;
a003_ = a000_ + 4;
a001_ = a001_ - a002_;
cur_ = 4 + cur_;
a004_ = 1 - a000_;
a002_ = 1 - a004_;
a000_ = a002_ + a004_;
a004_ = a001_ + a000_;
a003_ = cur_ - a004_;
a004_ = 1 - cur_;
a001_ = a002_ + cur_;
cur_ = a004_ + a000_;
} else {
}
a004_ = a004_ - a001_;
a001_ = -1 + a002_;
a000_ = a004_ - a003_;
a001_ = a004_ - 3;
a003_ = cur_ + cur_;
a003_ = 2 - -3;
a001_ = a004_ - a000_;
cur_ = -4 - cur_;
}
output.collect(prefix, new IntWritable(a001_));
}
