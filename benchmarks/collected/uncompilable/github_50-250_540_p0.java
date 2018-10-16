    DoubleWritable scoreWritable = new DoubleWritable();
    
    public enum iifCounter {COUNT};
    
    
    public static final String TOTAL_IMAGES = "org.sleuthkit.imagecount";
    public static final String FILES_IN_IMAGE = "org.sleuthkit.filecount";

    private long totalImages;
    private long filesInImage;
    
    JSONArray output = new JSONArray();
    
    
    public void setup(Context context) {
        totalImages = context.getConfiguration().getLong(TOTAL_IMAGES, -1);
        filesInImage = context.getConfiguration().getLong(FILES_IN_IMAGE, -1);
    }
    public void reduce(Writable key, Iterable<DoubleWritable> values, Context context) {
        JSONObject outputRecord = new JSONObject();
        double iif = 0;
        for (DoubleWritable iifu : values) {
            context.getCounter(iifCounter.COUNT).increment(1);
            iif = iif + iifu.get();
        }
        
        double confidence = iif/(Math.log((double)totalImages) * (double)filesInImage);
        
        try {
            outputRecord.put("id", new String(Hex.encodeHex(((BytesWritable)key).getBytes())));
            outputRecord.put("c", confidence);
            outputRecord.put("iif", iif);
            output.put(outputRecord);
        } catch (JSONException ex) { 
            ex.printStackTrace();
        }
    }
    
    protected void cleanup(Context context) {
        try {
            context.write(NullWritable.get(), new Text(output.toString()));
        } catch (Exception e) {
            
        }
    }


