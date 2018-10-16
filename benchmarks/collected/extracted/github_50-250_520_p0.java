
	//private static final Log _log = LogFactory.getLog(SsReducer.class);
	
	public void reduce(Text key, Iterable<DoubleWritable> values, Context context) throws IOException, InterruptedException {
		Text k = new Text(key.toString());
		int count = 0;
		
		Iterator<DoubleWritable> it = values.iterator();
		while(it.hasNext()) {
			Text v = new Text(it.next().toString());
			context.write(k, v);
			//_log.debug(k.toString() + " => " + v.toString());
			count++;
		}
		
		//_log.debug("count = " + count);
	}
