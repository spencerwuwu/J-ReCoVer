        public void reduce(Text key, Iterable<IntWritable> values, Context context) throws IOException, InterruptedException {
            //int delay = context.getConfiguration().getInt(DELAY, DEFAULT_DELAY);
            int sum = 0;
            for (IntWritable i: values){
                sum += i.get();
		/*
                if (k * KEY_NUMBER < j){
                    Thread.sleep(delay * 1000);
                    k++;
                }
		*/
                j+=i.get();
                
            }
//            Thread.sleep(3 + sum / delay);
            context.write(key, new IntWritable(sum));
        }

/*
        protected void cleanup(Context context) throws IOException, InterruptedException {
            super.cleanup(context);
            FileSystem fs = FileSystem.get(context.getConfiguration());
            
            Path p = new Path(new Path(WORKING_DIR, context.getJobName()), COMPARISON_DIR);
            Path w = new Path(p, context.getJobID().toString());
            FSDataOutputStream out = fs.create(new Path(new Path(w, "a"), getMachineName()));
            out.writeInt(j);
            out.close();

        }
	*/
