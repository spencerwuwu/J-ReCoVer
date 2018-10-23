
	
		  Text vword = new Text();
			public void reduce(Text key, Iterable<DoubleWritable> values, Context context) throws IOException, InterruptedException
			{
				double min = Double.MAX_VALUE;
				double max = 0.0;
				for(DoubleWritable value : values)
				{
					double current = value.get();
					max = (max>current)?max:current;
					min = (min<current)?min:current;
				}
				vword.set("Min: " + min + "\tMax: " + max);
				context.write(key, vword);
			}
			

