
		private Text outValue = new Text();
		
		public void reduce(Text key, Iterable<FloatWritable> values, Context context) throws IOException, InterruptedException {

			for (FloatWritable val : values) {

				outValue.set(val.toString());
				context.write(key, outValue);
			}
		}
