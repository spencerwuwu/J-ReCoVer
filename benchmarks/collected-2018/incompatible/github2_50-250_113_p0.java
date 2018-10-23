  String regexes[];

  final ByteArrayOutputStream buf = new ByteArrayOutputStream();
  JsonGenerator gen;

  protected void setup(Context context) throws IOException {
    // TODO: We could make sure that we only have one instance of this
    // here, since that's all I'm supporting at the moment.
    regexes = context.getConfiguration().get("mapred.mapper.regex").split("\n");

    gen = new JsonFactory().createJsonGenerator(buf, JsonEncoding.UTF8);
    gen.writeStartArray();
  }

  @SuppressWarnings("unchecked")
  public void reduce(LongWritable key, Iterable<LongWritable> values, Context context) throws IOException {
    gen.writeStartObject();

    int sum = 0;
    for (LongWritable value : values) {
      sum += value.get();
    }
  
    gen.writeNumberField("a", key.get());
    gen.writeNumberField("n", sum);
    gen.writeStringField("kw", regexes[(int) key.get()]);

    gen.writeEndObject();
  }

  protected void cleanup(Context context)
                                   throws IOException, InterruptedException {
    gen.writeEndArray();
    gen.close();
    context.write(NullWritable.get(), new Text(buf.toString("UTF-8")));
  }
