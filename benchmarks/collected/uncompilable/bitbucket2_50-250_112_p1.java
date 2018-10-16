        private Map<String, ByteBuffer> keys;
        private ByteBuffer key;
        protected void setup(org.apache.hadoop.mapreduce.Reducer.Context context)
        throws IOException, InterruptedException
        {
            keys = new LinkedHashMap<String, ByteBuffer>();
            String[] partitionKeys = context.getConfiguration().get(PRIMARY_KEY).split(",");
            keys.put("row_id1", ByteBufferUtil.bytes(partitionKeys[0]));
            keys.put("row_id2", ByteBufferUtil.bytes(partitionKeys[1]));
        }

        public void reduce(Text word, Iterable<IntWritable> values, Context context) throws IOException, InterruptedException
        {
            int sum = 0;
            for (IntWritable val : values)
                sum += val.get();
            context.write(keys, getBindVariables(word, sum));
        }

        private List<ByteBuffer> getBindVariables(Text word, int sum)
        {
            List<ByteBuffer> variables = new ArrayList<ByteBuffer>();
            keys.put("word", ByteBufferUtil.bytes(word.toString()));
            variables.add(ByteBufferUtil.bytes(String.valueOf(sum)));         
            return variables;
        }
