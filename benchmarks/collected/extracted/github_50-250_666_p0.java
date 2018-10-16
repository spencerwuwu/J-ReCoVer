
  //private static final Logger log = LoggerFactory.getLogger(BayesTfIdfReducer.class);

  public void reduce(Text key,
                     Iterator<DoubleWritable> values,
                     OutputCollector<Text, DoubleWritable> output,
                     Reporter reporter) throws IOException {
    //Key is label,word, value is the number of times we've seen this label word per local node.  Output is the same
    String token = key.toString();  
    if(token.startsWith("*vocabCount")) {
      double vocabCount = 0.0;
      while (values.hasNext()) {
        vocabCount += values.next().get();
      }
      //log.info("{}\t{}", token, vocabCount);
      output.collect(key, new DoubleWritable(vocabCount));
    } else {
      double idfTimes_D_ij = 1.0;
      //int numberofValues = 0;
      while (values.hasNext()) {
        idfTimes_D_ij *= values.next().get();
        //numberofValues ++;
      }
      //if(numberofValues!=2) throw new IOException("Number of values should be exactly 2");
      
      output.collect(key, new DoubleWritable(idfTimes_D_ij));
    }
  }
