		private static final FloatWritable value = new FloatWritable();
		private float marginal = 0.0f;

		public void reduce(MyTuple key, Iterable<FloatWritable> values, Context context) throws IOException, InterruptedException {
			float sum = 0.0f;
			Iterator<FloatWritable> iter = values.iterator();
			while (iter.hasNext()) {
				sum += iter.next().get();
			}

			try {
				if (key.getStringUnchecked("Right").equals("*")) {
					value.set(sum);
					context.write(key, value);
					marginal = sum;
				} else {
					value.set(sum / marginal);
					context.write(key, value);
				}
			} catch (JSONException e) {
				e.printStackTrace();
				throw new RuntimeException();
			}
		}
