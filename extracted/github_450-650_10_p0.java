    
    public void configure(JobConf job) {}
    
    public void close() {}
    
    private IndexDoc latest = new IndexDoc();
    
    public void reduce(Text key, Iterator<IndexDoc> values,
        OutputCollector<MD5Hash, IndexDoc> output, Reporter reporter) throws IOException {
      WritableUtils.cloneInto(latest, values.next());
      while (values.hasNext()) {
        IndexDoc value = values.next();
        if (value.time > latest.time) {
          // discard current and use more recent
          latest.keep = false;
          LOG.debug("-discard " + latest + ", keep " + value);
          output.collect(latest.hash, latest);
          WritableUtils.cloneInto(latest, value);
        } else {
          // discard
          value.keep = false;
          LOG.debug("-discard " + value + ", keep " + latest);
          output.collect(value.hash, value);
        }
        
      }
      // keep the latest
      latest.keep = true;
      output.collect(latest.hash, latest);
      
    }
