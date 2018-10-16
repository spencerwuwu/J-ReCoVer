  
  //private static final Logger log = LoggerFactory.getLogger(CBayesThetaReducer.class);
  
  public void reduce(Text key,
                     Iterator<DoubleWritable> values,
                     OutputCollector<Text, DoubleWritable> output,
                     Reporter reporter) throws IOException {
    //Key is label,word, value is the number of times we've seen this label word per local node.  Output is the same
    String token = key.toString();  
    double weight = 0.0;
    int numberofValues = 0;
    while (values.hasNext()) {
      weight += values.next().get();
      numberofValues ++;
    }    
    if(numberofValues < 2) return;    
    //if(weight <= 0.0)
      //log.info("{}=>{}", token, weight);
    output.collect(key, new DoubleWritable(weight));
  }

 
