    boolean byScore;
    
    public void configure(JobConf job) {
      byScore = job.getBoolean("dedup.keep.highest.score", true);
    }
    
    public void close() {}
    public void reduce(MD5Hash key, Iterator<IndexDoc> values,
                       OutputCollector<Text, IndexDoc> output, Reporter reporter)
      throws IOException {
      IndexDoc highest = null;
      while (values.hasNext()) {
        IndexDoc value = values.next();
        // skip already deleted
        if (!value.keep) {
          LOG.debug("-discard " + value + " (already marked)");
          output.collect(value.url, value);
          continue;
        }
        if (highest == null) {
          highest = value;
          continue;
        }
        IndexDoc toDelete = null, toKeep = null;
        boolean metric = byScore ? (value.score > highest.score) : 
                                   (value.urlLen < highest.urlLen);
        if (metric) {
          toDelete = highest;
          toKeep = value;
        } else {
          toDelete = value;
          toKeep = highest;
        }
        
        if (LOG.isDebugEnabled()) {
          LOG.debug("-discard " + toDelete + ", keep " + toKeep);
        }
        
        toDelete.keep = false;
        output.collect(toDelete.url, toDelete);
        highest = toKeep;
      }    
      LOG.debug("-keep " + highest);
      // no need to add this - in phase 2 we only process docs to delete them
      // highest.keep = true;
      // output.collect(key, highest);
    }
