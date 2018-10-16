	private static int K = 0;
    public static long HighKmer = 0;
    //private static int OVALSIZE = 0;
    private static int All_Kmer = 0;

	public void configure(JobConf job) {
		K = 24;//Integer.parseInt(job.get("K"));
        HighKmer = Long.parseLong(job.get("UP_KMER"));
	}

	public void reduce(Text prefix, Iterator<IntWritable> iter,
					   OutputCollector<Text, Text> output, Reporter reporter)
					   throws IOException
	{
        int sum =0;
        //int read_count = 0;
        List<String> ReadID_list = new ArrayList<String>();
        //List<String> ReadID_list;
        //Map<String, List<String>> idx_ReadID_list = new HashMap<String, List<String>>();
        while(iter.hasNext())
		{
        	int frequency = iter.next().get();
            sum = sum + frequency;
            //\\
            if (sum > HighKmer) {
            	output.collect(prefix, new Text(""));
            	//output.collect(new Text(Node.rc(prefix.toString())), new Text(""));
            	reporter.incrCounter("Brush", "hkmer", 1);
            	return;
            }
        }
	}
