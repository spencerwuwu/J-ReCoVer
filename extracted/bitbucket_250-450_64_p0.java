static int InDeg = 1, OutDeg = 2, InOutDeg = 3;

private final IntWritable one_int = new IntWritable(1);

int deg_type = 0;

public void configure(JobConf job) {
    deg_type = Integer.parseInt(job.get("deg_type"));

    System.out.println("RedPass1 : configure is called. degtype = " + deg_type );
}

public void reduce (final IntWritable key, final Iterator<IntWritable> values, OutputCollector<IntWritable, IntWritable> output, final Reporter reporter) throws IOException
{
    int degree = 0;

    if( deg_type != InOutDeg) {
        while (values.hasNext()) {
            int cur_degree = values.next().get();
            degree += cur_degree;
        }

        output.collect(key, new IntWritable(degree) );
    } else { // deg_type == InOutDeg
        Set<Integer> outEdgeSet = new TreeSet<Integer>();
        while (values.hasNext()) {
            int cur_outedge = values.next().get();
            outEdgeSet.add( cur_outedge );
        }

        output.collect(key, new IntWritable(outEdgeSet.size()) );
    }
}
