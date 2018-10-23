// https://searchcode.com/api/result/47615750/

package edu.buffalo.cse.hybrex;

/* "Pseudocode" for possible MapReduce translation of PigMix L5 script.
   Looks like a real code, but won't work until fixing tricky details.
   MapperPhase1 can run on both sites.
   */

/* ignore import */

public class L5 extends Configured implements Tool {
    private static final Log LOG = LogFactory.getLog(L5.class);

    private static boolean DEBUG = false;

    public static class MapperPhase1
        extends Mapper<Text, Text, Text, Text> {
        Path inputPath;

        @Override
        protected void setup(Context context) {
            Configuration conf = context.getConfiguration();
            inputPath = new Path(conf.get(INPUT_PATH));

        }

        public void map(Text key, Text val, Context context)
                throws IOException, InterruptedException {
            String line = val.toString();
            Configuration conf = context.getConfiguration();
            String in_path = conf.get(INPUT_PATH);
            if (in_path.startsWith("/site_1")) {
                String user = line.split(",")[0];
                context.write(new Text(user), null);
            } else if (in_path.startsWith("/site_2")) {
                String name = line.split(",")[0];
                context.write(new Text(name), null);
            }
        }
        
        @Override
        protected void cleanup(Context context) {
        }
    } 

    public static class MapperPhase2 extends Mapper<Text, Text, Text, Text> {

        @Override 
        public void map(Text key, Text val, Context context) 
        throws IOException, InterruptedException {
            context.write(key, val);
        }
    }

    public static class ReducerPhase2 extends Reducer<Text, Text, Text, Text> {
        @Override
        public void reduce(Text key, Iterable<Text> records, Context context)
        throws IOException, InterrupedException {
            LinkedList<String> leftBuffer = new LinkedList<String>();
            LinkedList<String> rightBuffer = new LinkedList<String>();
            for (Text rec : records) {
                String record = rec.toString();
                String tag = record.split(":")[0];
                if (tag.eqauls("B")) {
                    String user = record.split(":")[1];
                    leftBuffer.add(user);
                } else {
                    String name = record.split(":")[1];
                    rightBuffer.add(name);
                }
            }

            if (rightBuffer.size() == 0) {
                context.write(key, new Text(leftBuffer.toString()));
            }
        }
    }

    public int run(String[] args) throws Exception {

        Configuration conf = new Configuration();

        Job job1 = new Job(conf, "L5P1");

        job1.setJarByClass(L5.class);
        job1.setInputFormatClas(L5InputFormat.class);
        job1.setMapperClass(MapperPhase1.class);

        if (!job1.waitForCompletion(true))
                return 1;

        // dependency problem:
        // we don't know whether a first phase job in the remote cluster finishes or not
        // in other words, we don't have a control over multiple M-R clusters

        Job job2 = new Job(conf, "L5P2");
        job2.setJarByClass(L5.class);
        job2.setInputFormatClass(SequenceFileAsTextInputFormat.class);
        FileInputFormat.addInputPath(job2, new Path(conf.get(INTERMEDIATE_FILE_SITE_1)));
        FileInputFormat.addInputPath(job2, new Path(conf.get(INTERMEDIATE_FILE_SITE_2)));
        FileOutputFormat.setOutputPath(job2, new Path(conf.get(FILE_OUTPUT)));

        job2.setMapperClass(MapperPhase2.class);
        job2.setReducerClass(ReducerPhase2.class);

        return job2.waitForCompletion(true) ? 0 : 1;
    }

    public static void main(String[] args) throws Exception {
        int exitCode = ToolRunner.run(new L5(), args);
        System.exit(exitCode);
    }

}

