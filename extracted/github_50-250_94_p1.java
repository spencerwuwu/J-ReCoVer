
    JobConf _jobConf;
    @Override
    public void configure(JobConf job) {
      _jobConf = job;
    }

    @Override
    public void close() throws IOException {
      
    }

    @Override
    public void reduce(IntWritable key, Iterator<Text> values,OutputCollector<TextBytes, IntWritable> output, Reporter reporter)throws IOException {
      // collect all incoming paths first
      Vector<Path> incomingPaths = new Vector<Path>();

      while (values.hasNext()) {
        String path = values.next().toString();
        LOG.info("Found Incoming Path:" + path);
        incomingPaths.add(new Path(path));
      }

      // set up merge attributes
      Configuration localMergeConfig = new Configuration(_jobConf);

      localMergeConfig.setClass(MultiFileInputReader.MULTIFILE_COMPARATOR_CLASS, CrawlDBKey.LinkKeyComparator.class,
          RawComparator.class);
      localMergeConfig.setClass(MultiFileInputReader.MULTIFILE_KEY_CLASS, TextBytes.class, WritableComparable.class);

      FileSystem fs = FileSystem.get(incomingPaths.get(0).toUri(),_jobConf);
      
      // ok now spawn merger
      MultiFileInputReader<TextBytes> multiFileInputReader 
        = new MultiFileInputReader<TextBytes>(fs, incomingPaths, localMergeConfig);

      try {
        
        Pair<KeyAndValueData<TextBytes>, Iterable<RawRecordValue>> nextItem = null;
    
        TextBytes valueText = new TextBytes();
        DataInputBuffer valueStream = new DataInputBuffer();
        JsonParser parser = new JsonParser();
        IntWritable one = new IntWritable(1);
        while ((nextItem = multiFileInputReader.getNextItemIterator()) != null) {
        
          long recordType = CrawlDBKey.getLongComponentFromKey(nextItem.e0._keyObject, CrawlDBKey.ComponentId.TYPE_COMPONENT_ID);
          if (recordType == CrawlDBKey.Type.KEY_TYPE_MERGED_RECORD.ordinal()) {
            reporter.incrCounter(Counters.GOT_MERGED_RECORD, 1);
            // walk records
            RawRecordValue rawValue = Iterators.getNext(nextItem.e1.iterator(),null);
            if (rawValue != null) {
              valueStream.reset(rawValue.data.getData(),0,rawValue.data.getLength());
              valueText.setFromRawTextBytes(valueStream);
              try {
                JsonObject mergeRecord = parser.parse(valueText.toString()).getAsJsonObject();
                if (mergeRecord.has(CrawlDBCommon.TOPLEVEL_LINKSTATUS_PROPERTY)) { 
                  JsonObject linkStatus = mergeRecord.getAsJsonObject(CrawlDBCommon.TOPLEVEL_LINKSTATUS_PROPERTY);
                  if (linkStatus.has(CrawlDBCommon.LINKSTATUS_TYPEANDRELS_PROPERTY)) { 
                    JsonArray typeAndRelsArray = linkStatus.getAsJsonArray(CrawlDBCommon.LINKSTATUS_TYPEANDRELS_PROPERTY);
                    for (JsonElement typeAndRel : typeAndRelsArray) {
                      output.collect(new TextBytes(typeAndRel.getAsString()), one);
                    }
                  }
                }
              }
              catch (Exception e) {
                reporter.incrCounter(Counters.HIT_EXCEPTION_PROCESSING_RECORD, 1);
                LOG.error(CCStringUtils.stringifyException(e));
              }
            }
          }
        }
      }
      finally { 
        multiFileInputReader.close();
      }      
    }       
    
    
