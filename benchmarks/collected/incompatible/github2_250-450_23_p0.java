
    private int numVertices;
    private double teleportationProbability;
    private Vector weights;

    protected void setup(Context ctx) throws IOException, InterruptedException {
      Configuration conf = ctx.getConfiguration();
      Path indexedDegreesPath = new Path(ctx.getConfiguration().get(INDEXED_DEGREES_PARAM));
      numVertices = Integer.parseInt(conf.get(NUM_VERTICES_PARAM));
      teleportationProbability = Double.parseDouble(conf.get(TELEPORTATION_PROBABILITY_PARAM));
      Preconditions.checkArgument(numVertices > 0);
      Preconditions.checkArgument(teleportationProbability > 0 && teleportationProbability < 1);
      weights = new DenseVector(numVertices);

      for (Pair<IntWritable, IntWritable> indexAndDegree :
          new SequenceFileDirIterable<IntWritable, IntWritable>(indexedDegreesPath, PathType.LIST,
          PathFilters.partFilter(), ctx.getConfiguration())) {
        weights.set(indexAndDegree.getFirst().get(), 1.0 / indexAndDegree.getSecond().get());
      }
    }

    public void reduce(IntWritable vertexIndex, Iterable<IntWritable> incidentVertexIndexes, Context ctx)
        throws IOException, InterruptedException {
      Vector vector = new RandomAccessSparseVector(numVertices);
      for (IntWritable incidentVertexIndex : incidentVertexIndexes) {
        double weight = weights.get(incidentVertexIndex.get()) * teleportationProbability;
        //System.out.println(vertexIndex.get() + "," + incidentVertexIndex.get() + ": " + weight);
        vector.set(incidentVertexIndex.get(), weight);
      }
      ctx.write(vertexIndex, new VectorWritable(vector));
    }
