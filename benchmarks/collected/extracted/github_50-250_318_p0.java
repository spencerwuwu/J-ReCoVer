/*
		Map<String, String> info = null;
		public void setup(Context context) throws IOException
        {
                loadKeys(context);
        }
        void loadKeys(Context context) throws IOException
        {
                FSDataInputStream in = null;
                BufferedReader br = null;
                FileSystem fs = FileSystem.get(context.getConfiguration());
                Path path = new Path(filePath);
                in = fs.open(path);
                br  = new BufferedReader(new InputStreamReader(in));
                info = new HashMap<String, String>();
                String line = "";
                while ( (line = br.readLine() )!= null) {
                String[] arr = line.split("\\,");
                if (arr.length == 2)
                	info.put(arr[0], arr[1]);
                }
                in.close();
        }
	*/
        Text kword = new Text();
		LongWritable vword = new LongWritable();
		public void reduce(Text key, Iterable<LongWritable> values, Context context) throws IOException, InterruptedException
		{
			long sum = 0;
			for(LongWritable value : values)
			{
				sum = sum + value.get();
			}
			//String myKey = key.toString() + "\t" + info.get(key.toString());
			String myKey = key.toString() + "\t";
			kword.set(myKey);
			vword.set(sum);
			context.write(kword, vword);
		}
