        private ByteBuffer outputKey;

        protected void setup(org.apache.hadoop.mapreduce.Reducer.Context context)
        throws IOException, InterruptedException
        {
            outputKey = ByteBufferUtil.bytes("word_count_22");
        }

        public void reduce(Text word, Iterable<IntWritable> values, Context context) throws IOException, InterruptedException
        {
            int sum = 0;
            for (IntWritable val : values)
                sum += val.get();
            if (sum >= 1) {
            	//context.write(outputKey, Collections.singletonList(getMutation(word, sum)));
            	context.write(outputKey, Collections.singletonList(getMutation(word, sum)));
            }
        }

        private static Mutation getMutation(Text word, int sum)
        {
            Column c = new Column();
            
            c.setName(Arrays.copyOf(word.getBytes(), word.getLength()));
            c.setValue(ByteBufferUtil.bytes(String.valueOf(sum)));
            c.setTimestamp(System.currentTimeMillis());

            Mutation m = new Mutation();
            m.setColumn_or_supercolumn(new ColumnOrSuperColumn());
            m.column_or_supercolumn.setColumn(c);
            return m;
        }
        
        
        private static Mutation getCounterMutation(Text word, int sum) {
        	CounterColumn counter = new CounterColumn();
        	counter.setName(Arrays.copyOf(word.getBytes(), word.getLength()));
        	counter.setValue(Long.valueOf(sum));
        	Mutation m = new Mutation();
            m.setColumn_or_supercolumn(new ColumnOrSuperColumn());
            m.column_or_supercolumn.setCounter_column(counter);
            return m;
        }
