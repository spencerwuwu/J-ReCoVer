			    
		IndexedFloatArray scores = new IndexedFloatArray(2);
			  
		public void reduce(PhrasePair key, Iterator<IndexedFloatArray> values,
				OutputCollector<PhrasePair, IndexedFloatArray> output, 
				Reporter reporter) throws IOException {
			scores.clear();
			while (values.hasNext()) {
				scores.plusEquals(values.next());
			}
			output.collect(key, scores);
		}
