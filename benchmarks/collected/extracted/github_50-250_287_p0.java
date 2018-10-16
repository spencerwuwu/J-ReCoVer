        private IntWritable v = new IntWritable();
        private int money = 0;

        public void reduce(Text key, Iterable<IntWritable> values, Context context) throws IOException, InterruptedException {
            for (IntWritable line : values) {
                // System.out.println(key.toString() + "\t" + line);
                money += line.get();
            }
            v.set(money);
            context.write(null, v);
            System.out.println("Output:" + key + "," + money);
        }

