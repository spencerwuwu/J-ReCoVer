
    JobConf _conf;
    FileSystem _fs;
    long _rootTimestamp;

    private static final NumberFormat NUMBER_FORMAT = NumberFormat.getInstance();
    static {
      NUMBER_FORMAT.setMinimumIntegerDigits(5);
      NUMBER_FORMAT.setGroupingUsed(false);
    }

    enum Counters {
      HAD_ONLY_METADATA, HAD_ONLY_PR, HAD_ONLY_LINKDATA, HAD_ONLY_INVERSELINKDATA, HAD_METADATA_AND_PR,
      HAD_METADATA_AND_LINKDATA, HAD_METADATA_AND_INVERSELINKDATA, HAD_METADATA_AND_ALL_LINKDATA, HAD_ALL_DATA,
      HAD_METADATA_PR_AND_LINKDATA, HAD_METADATA_PR_AND_INVERSE_LINKDATA, HAD_LINKDATA_AND_INVERSE_LINKDATA,
      HAD_PR_LINKDATA_AND_INVERSE_LINKDATA, HAD_ONLY_URL, HAD_REDIRECT_LOCATION_IN_METADATA, FOUND_S3_ARCHIVEINFO,
      FOUND_CRAWLONE_ARCHIVE_INFO, FOUND_CURRENTCRAWL_ARCHIVE_INFO, FOUND_UNKNOWNCRAWL_ARCHIVE_INFO,
      URLDB_RECORD_WITHOUT_URL, HAD_URL, WROTE_SINGLE_RECORD, HAD_INV_LINKDB_DATA, HAD_LINKDB_DATA, HAD_PAGERANK

    }

    static final String FP_TO_PR_PATTERN = "fpToPR";
    static final String LINKDB_PATTERN = "linkdb/merged";
    static final String INVERSE_LINKDB_PATTERN = "inverse_linkdb/merged";
    static final String CRAWLDB_METADATA_PATTERN = "/urlMetadata/seed";
    static final String S3_METADATA_PATTERN = "/s3Metadata/";
    static final String URLDB_PATTERN = "/crawl/crawldb_new";

    static final int HAD_METADATA = 1 << 0;
    static final int HAD_PR = 1 << 1;
    static final int HAD_LINKDATA = 1 << 2;
    static final int HAD_INVERSE_LINKDATA = 1 << 3;
    static final int HAD_URL = 1 << 4;

    static final int MASK_HAD_METADATA_AND_PR = HAD_URL | HAD_METADATA | HAD_PR;
    static final int MASK_HAD_METADATA_AND_LINKDATA = HAD_URL | HAD_METADATA | HAD_LINKDATA;
    static final int MASK_HAD_METADATA_AND_INVERSE_LINKDATA = HAD_URL | HAD_METADATA | HAD_INVERSE_LINKDATA;
    static final int MASK_HAD_METADATA_AND_ALL_LINKDATA = HAD_URL | HAD_METADATA | HAD_LINKDATA | HAD_INVERSE_LINKDATA;
    static final int MASK_HAD_METADATA_PR_AND_LINKDATA = HAD_URL | HAD_METADATA | HAD_LINKDATA | HAD_PR;
    static final int MASK_HAD_METADATA_PR_AND_INVERSE_LINKDATA = HAD_URL | HAD_METADATA | HAD_INVERSE_LINKDATA | HAD_PR;
    static final int MASK_HAD_ALL_DATA = HAD_URL | HAD_METADATA | HAD_LINKDATA | HAD_INVERSE_LINKDATA | HAD_PR;
    static final int MASK_HAD_LINKDATA_AND_INVERSE_LINKDATA = HAD_LINKDATA | HAD_INVERSE_LINKDATA;
    static final int MASK_HAD_PR_AND_LINKDATA_AND_INVERSE_LINKDATA = HAD_PR | HAD_LINKDATA | HAD_INVERSE_LINKDATA;

    @Override
    public void reduce(IntWritable key, Iterator<Text> values, OutputCollector<NullWritable, NullWritable> output,
        Reporter reporter) throws IOException {

      // collect all incoming paths first
      Vector<Path> incomingPaths = new Vector<Path>();

      while (values.hasNext()) {

        String path = values.next().toString();
        LOG.info("Found Incoming Path:" + path);
        incomingPaths.add(new Path(path));
      }

      // set up merge attributes
      JobConf localMergeConfig = new JobConf(_conf);

      localMergeConfig.setClass(MultiFileInputReader.MULTIFILE_COMPARATOR_CLASS, URLFPV2RawComparator.class,
          RawComparator.class);
      localMergeConfig.setClass(MultiFileInputReader.MULTIFILE_KEY_CLASS, URLFPV2.class, WritableComparable.class);

      // ok now spawn merger
      MultiFileInputReader<URLFPV2> multiFileInputReader = new MultiFileInputReader<URLFPV2>(_fs, incomingPaths,
          localMergeConfig);

      // now read one set of values at a time and output result
      KeyAndValueData<URLFPV2> keyValueData = null;

      DataOutputBuffer builderOutputBuffer = new DataOutputBuffer();

      // create output paths ...
      // Path outputDataFile = new
      // Path(FileOutputFormat.getWorkOutputPath(_conf),"part-" +
      // NUMBER_FORMAT.format(key.get()));
      Path outputDataFile = new Path(FileOutputFormat.getWorkOutputPath(_conf), "part-"
          + NUMBER_FORMAT.format(key.get()) + ".data");
      Path outputIndexFile = new Path(FileOutputFormat.getWorkOutputPath(_conf), "part-"
          + NUMBER_FORMAT.format(key.get()) + ".index");

      Path domainMetadataIndexFile = new Path(FileOutputFormat.getWorkOutputPath(_conf), "part-"
          + NUMBER_FORMAT.format(key.get()) + ".domainMetadata");

      LOG.info("Creating TFile Index at:" + outputDataFile);
      LOG.info("Creating DomainMetadata File at:" + domainMetadataIndexFile);

      // create output streams. ...
      // FSDataOutputStream dataStream = _fs.create(outputDataFile);
      FSDataOutputStream dataStream = _fs.create(outputDataFile);
      FSDataOutputStream indexStream = _fs.create(outputIndexFile);

      try {
        // and create tfile writer
        // TFile.Writer indexWriter = new TFile.Writer(dataStream,64 *
        // 1024,TFile.COMPRESSION_LZO,TFile.COMPARATOR_JCLASS +
        // TFileBugWorkaroundDomainHashAndURLHashComparator.class.getName(),
        // _conf);
        CompressURLListV2.Builder builder = new CompressURLListV2.Builder(indexStream, dataStream);

        try {
          // sub domain metadata writer
          SequenceFile.Writer domainMetadataWriter = SequenceFile.createWriter(_fs, _conf, domainMetadataIndexFile,
              LongWritable.class, SubDomainMetadata.class);

          try {

            DataOutputBuffer finalOutputBuffer = new DataOutputBuffer();
            DataInputBuffer inputBuffer = new DataInputBuffer();
            TriTextBytesTuple tupleOut = new TriTextBytesTuple();
            DataOutputBuffer fastLookupBuffer = new DataOutputBuffer();
            SubDomainMetadata metadata = null;
            // LongTextBytesTuple urlTuple = new LongTextBytesTuple();

            DataOutputBuffer datumStream = new DataOutputBuffer();
            DataOutputBuffer keyBuffer = new DataOutputBuffer();
            TextBytes textValueBytes = new TextBytes();

            // start reading merged values ...
            while ((keyValueData = multiFileInputReader.readNextItem()) != null) {

              // ok metadata we are going to write into
              CrawlDatumAndMetadata datumAndMetadata = new CrawlDatumAndMetadata();
              TextBytes urlBytes = null;
              Vector<ArchiveInfo> s3Items = new Vector<ArchiveInfo>();
              boolean dirty = false;

              int mask = 0;

              metadata = createOrFlushSubDomainMetadata(keyValueData._keyObject, metadata, domainMetadataWriter);

              // increment url count
              metadata.setUrlCount(metadata.getUrlCount() + 1);

              // walk values ...
              for (RawRecordValue value : keyValueData._values) {

                String path = value.source.toString();
                inputBuffer.reset(value.data.getData(), value.data.getLength());

                if (path.contains(S3_METADATA_PATTERN)) {
                  ArchiveInfo s3ArchiveInfo = new ArchiveInfo();
                  s3ArchiveInfo.readFields(inputBuffer);
                  s3Items.add(s3ArchiveInfo);
                  reporter.incrCounter(Counters.FOUND_S3_ARCHIVEINFO, 1);
                } else if (path.contains(URLDB_PATTERN)) {

                  mask |= HAD_METADATA;

                  datumAndMetadata.readFields(inputBuffer);

                  urlBytes = datumAndMetadata.getUrlAsTextBytes();

                  if (urlBytes != null && urlBytes.getLength() != 0) {

                    mask |= HAD_URL;

                    if (!metadata.isFieldDirty(SubDomainMetadata.Field_DOMAINTEXT)) {
                      String url = urlBytes.toString();
                      String domain = URLUtils.fastGetHostFromURL(url);
                      if (domain != null && domain.length() != 0) {
                        metadata.setDomainText(domain);
                      }
                    }

                    // update subdomain metadata
                    switch (datumAndMetadata.getStatus()) {
                      case CrawlDatum.STATUS_DB_UNFETCHED:
                        metadata.setUnfetchedCount(metadata.getUnfetchedCount() + 1);
                        break;
                      case CrawlDatum.STATUS_DB_FETCHED:
                        metadata.setFetchedCount(metadata.getFetchedCount() + 1);
                        break;
                      case CrawlDatum.STATUS_DB_GONE:
                        metadata.setGoneCount(metadata.getGoneCount() + 1);
                        break;
                      case CrawlDatum.STATUS_DB_REDIR_TEMP:
                        metadata.setRedirectTemporaryCount(metadata.getRedirectTemporaryCount() + 1);
                        break;
                      case CrawlDatum.STATUS_DB_REDIR_PERM:
                        metadata.setRedirectPermCount(metadata.getRedirectPermCount() + 1);
                        break;
                      case CrawlDatum.STATUS_DB_NOTMODIFIED:
                        metadata.setUnmodifiedCount(metadata.getUnmodifiedCount() + 1);
                        break;
                    }

                    // update fetch time stats
                    metadata.setLatestFetchTime(Math
                        .max(metadata.getLatestFetchTime(), datumAndMetadata.getFetchTime()));

                    CrawlURLMetadata metadataObj = (CrawlURLMetadata) datumAndMetadata.getMetadata();

                    // clear some invalid fields ...
                    metadataObj.setFieldClean(CrawlURLMetadata.Field_SIGNATURE);
                    metadataObj.setFieldClean(CrawlURLMetadata.Field_HOSTFP);
                    metadataObj.setFieldClean(CrawlURLMetadata.Field_URLFP);
                    metadataObj.setFieldClean(CrawlURLMetadata.Field_CONTENTFILESEGNO);
                    metadataObj.setFieldClean(CrawlURLMetadata.Field_CONTENTFILENAMEANDPOS);
                    metadataObj.setFieldClean(CrawlURLMetadata.Field_PARSEDATASEGNO);
                    metadataObj.setFieldClean(CrawlURLMetadata.Field_CRAWLNUMBER);
                    metadataObj.setFieldClean(CrawlURLMetadata.Field_PARSENUMBER);
                    metadataObj.setFieldClean(CrawlURLMetadata.Field_UPLOADNUMBER);
                    metadataObj.setFieldClean(CrawlURLMetadata.Field_ARCFILEDATE);
                    metadataObj.setFieldClean(CrawlURLMetadata.Field_ARCFILEINDEX);
                    metadataObj.setFieldClean(CrawlURLMetadata.Field_ARCFILENAME);
                    metadataObj.setFieldClean(CrawlURLMetadata.Field_ARCFILEOFFSET);
                    metadataObj.setFieldClean(CrawlURLMetadata.Field_ARCFILESIZE);
                    metadataObj.setFieldClean(CrawlURLMetadata.Field_LINKDBTIMESTAMP);
                    metadataObj.setFieldClean(CrawlURLMetadata.Field_INVERSEDBTIMESTAMP);
                    metadataObj.setFieldClean(CrawlURLMetadata.Field_INVERSEDBEXTRADOMAININLINKCOUNT);
                    metadataObj.setFieldClean(CrawlURLMetadata.Field_INVERSEDBINTRADOMAININLINKCOUNT);
                    metadataObj.setFieldClean(CrawlURLMetadata.Field_PAGERANKTIMESTAMP);
                    metadataObj.setFieldClean(CrawlURLMetadata.Field_PAGERANKVALUEOLD);
                    metadataObj.getParseSegmentInfo().clear();
                    // clear url field as we store it in a separate location ...
                    metadataObj.setFieldClean(CrawlDatumAndMetadata.Field_URL);

                    // update sub domain stats ...
                    if (metadataObj.getArchiveInfo().size() != 0) {
                      metadata.setHasArcFileInfoCount(metadata.getHasArcFileInfoCount() + 1);
                    }
                    if (metadataObj.getParseSegmentInfo().size() != 0) {
                      metadata.setHasParseSegmentInfoCount(metadata.getHasParseSegmentInfoCount() + 1);
                    }
                    if (metadataObj.isFieldDirty(CrawlURLMetadata.Field_PAGERANK)) {
                      reporter.incrCounter(Counters.HAD_PAGERANK, 1);
                      metadata.setHasPageRankCount(metadata.getHasPageRankCount() + 1);
                    }
                    if (metadataObj.isFieldDirty(CrawlURLMetadata.Field_LINKDBFILENO)) {
                      reporter.incrCounter(Counters.HAD_LINKDB_DATA, 1);
                      metadata.setHasLinkListCount(metadata.getHasLinkListCount() + 1);
                    }
                    if (metadataObj.isFieldDirty(CrawlURLMetadata.Field_INVERSEDBFILENO)) {
                      reporter.incrCounter(Counters.HAD_INV_LINKDB_DATA, 1);
                      metadata.setHasInverseLinkListCount(metadata.getHasInverseLinkListCount() + 1);
                    }

                    dirty = true;
                  } else {
                    reporter.incrCounter(Counters.URLDB_RECORD_WITHOUT_URL, 1);
                  }
                }
              }

              // URL IS VALID
              if (((mask & HAD_URL) != 0)) {

                reporter.incrCounter(Counters.HAD_URL, 1);
                // add any s3 archive information
                datumAndMetadata.getMetadata().getArchiveInfo().addAll(s3Items);

                // ok only keep last value archive info
                ArchiveInfo lastValidArchiveInfo = null;
                int archiveInfoCount = 0;
                for (ArchiveInfo archiveInfo : datumAndMetadata.getMetadata().getArchiveInfo()) {
                  if (lastValidArchiveInfo == null
                      || lastValidArchiveInfo.getArcfileDate() < archiveInfo.getArcfileDate()) {
                    lastValidArchiveInfo = archiveInfo;
                  }
                  ++archiveInfoCount;
                }

                if (lastValidArchiveInfo != null) {
                  // clear archive info
                  datumAndMetadata.getMetadata().getArchiveInfo().clear();
                  datumAndMetadata.getMetadata().getArchiveInfo().add(lastValidArchiveInfo);

                  if (lastValidArchiveInfo.getCrawlNumber() == 1) {
                    reporter.incrCounter(Counters.FOUND_CRAWLONE_ARCHIVE_INFO, 1);
                  } else if (lastValidArchiveInfo.getCrawlNumber() == CrawlEnvironment.getCurrentCrawlNumber()) {
                    reporter.incrCounter(Counters.FOUND_CURRENTCRAWL_ARCHIVE_INFO, 1);
                  } else {
                    reporter.incrCounter(Counters.FOUND_UNKNOWNCRAWL_ARCHIVE_INFO, 1);
                  }
                }

                // ok initialize tuple .. first value is url ...
                tupleOut.setFirstValue(new TextBytes(urlBytes));

                if (dirty) {
                  datumAndMetadata.setIsValid((byte) 1);
                  // second value is special
                  fastLookupBuffer.reset();
                  // write page rank value
                  fastLookupBuffer.writeFloat(datumAndMetadata.getMetadata().getPageRank());
                  // write fetch status
                  fastLookupBuffer.writeByte(datumAndMetadata.getStatus());
                  // protocol status
                  fastLookupBuffer.writeByte(datumAndMetadata.getProtocolStatus());
                  // write fetch time
                  fastLookupBuffer.writeLong(datumAndMetadata.getFetchTime());
                  // ok write this buffer into second tuple value
                  tupleOut.getSecondValue().set(fastLookupBuffer.getData(), 0, fastLookupBuffer.getLength());
                  // ok write out datum and metadata to stream
                  datumStream.reset();
                  datumAndMetadata.write(datumStream);
                  // set third value in output tuple
                  tupleOut.getThirdValue().set(datumStream.getData(), 0, datumStream.getLength());
                } else {
                  tupleOut.getSecondValue().clear();
                  tupleOut.getThirdValue().clear();
                }

                // reset composite buffer
                finalOutputBuffer.reset();
                // write tuple into it -- TODO: DOUBLE BUFFER COPIES SUCK!!!
                tupleOut.write(finalOutputBuffer);
                // write out key value
                keyBuffer.reset();
                keyBuffer.writeLong(keyValueData._keyObject.getDomainHash());
                keyBuffer.writeLong(keyValueData._keyObject.getUrlHash());

                textValueBytes.set(finalOutputBuffer.getData(), 0, finalOutputBuffer.getLength());
                // output final value to index builder ...
                /*
                 * indexWriter.append( keyBuffer.getData(), 0,
                 * keyBuffer.getLength(), finalOutputBuffer.getData(), 0,
                 * finalOutputBuffer.getLength());
                 */

                reporter.incrCounter(Counters.WROTE_SINGLE_RECORD, 1);
                builder.addItem(keyValueData._keyObject, textValueBytes);

                // update stats
                if (mask == HAD_URL) {
                  reporter.incrCounter(Counters.HAD_ONLY_URL, 1);
                } else if (mask == HAD_METADATA) {
                  reporter.incrCounter(Counters.HAD_ONLY_METADATA, 1);
                } else if (mask == HAD_PR) {
                  reporter.incrCounter(Counters.HAD_ONLY_PR, 1);
                } else if (mask == HAD_LINKDATA) {
                  reporter.incrCounter(Counters.HAD_ONLY_LINKDATA, 1);
                } else if (mask == HAD_INVERSE_LINKDATA) {
                  reporter.incrCounter(Counters.HAD_ONLY_INVERSELINKDATA, 1);
                } else if (mask == MASK_HAD_METADATA_AND_PR) {
                  reporter.incrCounter(Counters.HAD_METADATA_AND_PR, 1);
                } else if (mask == MASK_HAD_METADATA_AND_LINKDATA) {
                  reporter.incrCounter(Counters.HAD_METADATA_AND_LINKDATA, 1);
                } else if (mask == MASK_HAD_METADATA_AND_INVERSE_LINKDATA) {
                  reporter.incrCounter(Counters.HAD_METADATA_AND_INVERSELINKDATA, 1);
                } else if (mask == MASK_HAD_METADATA_AND_ALL_LINKDATA) {
                  reporter.incrCounter(Counters.HAD_METADATA_AND_ALL_LINKDATA, 1);
                } else if (mask == MASK_HAD_METADATA_PR_AND_LINKDATA) {
                  reporter.incrCounter(Counters.HAD_METADATA_PR_AND_LINKDATA, 1);
                } else if (mask == MASK_HAD_METADATA_PR_AND_INVERSE_LINKDATA) {
                  reporter.incrCounter(Counters.HAD_METADATA_PR_AND_INVERSE_LINKDATA, 1);
                } else if (mask == MASK_HAD_ALL_DATA) {
                  reporter.incrCounter(Counters.HAD_ALL_DATA, 1);
                } else if (mask == MASK_HAD_LINKDATA_AND_INVERSE_LINKDATA) {
                  reporter.incrCounter(Counters.HAD_LINKDATA_AND_INVERSE_LINKDATA, 1);
                } else if (mask == MASK_HAD_PR_AND_LINKDATA_AND_INVERSE_LINKDATA) {
                  reporter.incrCounter(Counters.HAD_PR_LINKDATA_AND_INVERSE_LINKDATA, 1);
                }
              }

              // report progress to keep reducer alive
              reporter.progress();
            }
            // flush trailing domain metadata entry ...
            if (metadata != null) {
              domainMetadataWriter.append(new LongWritable(metadata.getDomainHash()), metadata);
            }
          } finally {
            domainMetadataWriter.close();
          }
        } finally {
          // indexWriter.close();
          builder.close();
        }
      } finally {
        dataStream.close();
        indexStream.close();
      }
    }

    @Override
    public void configure(JobConf job) {
      _conf = job;
      try {
        _fs = FileSystem.get(_conf);
        _rootTimestamp = job.getLong("root.timestamp", -1);
      } catch (IOException e) {
        LOG.error(CCStringUtils.stringifyException(e));
      }

    }

    @Override
    public void close() throws IOException {
      // TODO Auto-generated method stub

    }

    private static SubDomainMetadata createOrFlushSubDomainMetadata(URLFPV2 currentFP, SubDomainMetadata metadata,
        SequenceFile.Writer writer) throws IOException {
      if (metadata != null) {
        // if current fingerprint is for a different subdomain ..
        if (currentFP.getDomainHash() != metadata.getDomainHash()) {
          // flush the existing record
          writer.append(new LongWritable(metadata.getDomainHash()), metadata);
          // null out metadata
          metadata = null;
        }
      }
      // if metadata is null ...
      if (metadata == null) {
        // allocate a fresh new record for this new subdomain
        metadata = new SubDomainMetadata();
        metadata.setDomainHash(currentFP.getDomainHash());
        metadata.setRootDomainHash(currentFP.getRootDomainHash());
      }
      return metadata;
    }

