//https://searchcode.com/file/10576200/hadoop-mapreduce-project/hadoop-mapreduce-client/hadoop-mapreduce-client-jobclient/src/test/java/org/apache/hadoop/mapred/GenericMRLoadGenerator.java#l-42

	private long total;

	private long kept = 0;

	private float keep;

	protected void emit(LongWritable key, LongWritable val, OutputCollector<LongWritable,LongWritable> out)
		throws IOException {		++total;
		while((float) kept / total < keep) {
		++kept;
			out.collect(key, val);
		}
	}
	public void reduce(LongWritable key, Iterator<LongWritable> values,
			OutputCollector<LongWritable,LongWritable> output, Reporter reporter)
					throws IOException {
		while (values.hasNext()) {
			emit(key, values.next(), output);
		}
	}

