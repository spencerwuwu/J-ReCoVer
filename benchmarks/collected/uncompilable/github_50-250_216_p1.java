		private static final FloatWritable value = new FloatWritable();
		private float marginal = 0.0f;

		public void reduce(PairOfStrings key, Iterable<FloatWritable> values, Context context) throws IOException, InterruptedException {
			float sum = 0.0f;
			Iterator<FloatWritable> iter = values.iterator();
			while (iter.hasNext()) {
				sum += iter.next().get();
			}

			if (key.getRightElement().equals("*")) {
				value.set(sum);
				context.write(key, value);
				marginal = sum;
			} else {
				value.set(sum / marginal);
				context.write(key, value);
			}
		}
