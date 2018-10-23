
  //private final Logger log = LoggerFactory.getLogger(CBayesNormalizedWeightReducer.class);      

  public void reduce(Text key,
                     Iterator<DoubleWritable> values,
                     OutputCollector<Text, DoubleWritable> output,
                     Reporter reporter) throws IOException {
    //Key is label,word, value is the number of times we've seen this label word per local node.  Output is the same
    String token = key.toString();  
    double weight = 0.0;
    while (values.hasNext()) {
      weight += values.next().get();
    }
    //if(token.equalsIgnoreCase("rec.motorcycles,miller"))
      //log.info("{}=>{}", token, weight);
    output.collect(key, new DoubleWritable(weight));
  }

 
