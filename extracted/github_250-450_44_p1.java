
float marginal = 0.0f;
int need_to_cover = 0;
FloatWritable prob = new FloatWritable(0.0f);

public void reduce(Text key, Iterator<IntWritable> values,
        OutputCollector<Text, FloatWritable> output, 
        Reporter reporter) throws IOException {
    // if (!values.hasNext()) throw new UnexpectedException("no values for " + key);
    int v = values.next().get();
    if (need_to_cover == 0) {
        // if (key.getE().size() != 0) throw new UnexpectedException("Expected empty e-side: " + key);
        need_to_cover = v;
        // if (v < 1) throw new UnexpectedException("Bad count: " + v);
        marginal = (float)v;
    } else {
        // if (key.getE().size() == 0) throw new UnexpectedException("unaccounted for counts: " + need_to_cover + " key=" +key);
        float p = (float)v / marginal;
        prob.set(p);
        output.collect(key, prob);
        need_to_cover -= v;
    }			    
}
