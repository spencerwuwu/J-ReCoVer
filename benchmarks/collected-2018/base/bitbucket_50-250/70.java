// https://searchcode.com/api/result/134046553/

/*
 * <p>
 * Last(8th) stage for document similarity calculation.
 * Generates articles and their similar articles.
 * </p>
 *
 * <p>
 * A sample invocation:
 * </p>
 *
 * <blockquote>
 *
 * <pre>
 *      hadoop jar ./wikiMiner-deps.jar edu.macademia.wikiminer.JSONDocSim \
 *              -D mapred.map.tasks=100 -D mapred.reduce.tasks=20 \
 *              /user/shilad/macademia/res/6 \
 *              /user/shilad/macademia/res/8/
 * </pre>
 *
 * </blockquote>
 */

package wmr.tfidf;


import java.io.IOException;
import java.util.Iterator;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import wikiParser.mapReduce.util.KeyValueTextInputFormat;
import wmr.util.Utils;

/**
 *
 * @author equeirosnunes
 */
public class Step6DocSimFormatter extends Configured implements Tool{
    private static final int MAX_DOCS = 20000;

     /**
     * Key:articleID , Value: articleID1,score | articleID2,score ...
     */
    private static class MyMapper extends Mapper<Text, Text, Text, Text> {

        @Override
        public void map(Text key, Text value, Context context) throws IOException, InterruptedException {
            String articlePair = key.toString();
            String[] tokens = articlePair.split("@");

            if(tokens.length == 2){
                String article1 = tokens[0];
                String article2 = tokens[1];
                String score = value.toString();
                context.write(new Text(article1), new Text(article2 +"@"+ score));
            } else {
                System.err.println("invalid key/value pair: " + key + ", " + value);
            }

        }
    }


    /**
     * Prepends a word#c containing the total count for the word across all
     * wikipedia articles.
     */
    private static class MyReducer extends Reducer<Text, Text, Text, Text> {

        @Override
        public void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException {

            TopScoreQueue pqueue = new TopScoreQueue(MAX_DOCS);
            for (Text t : values) {
                try {
                    String [] tokens = t.toString().split("@");
                    String article = tokens[0];
                    double score = Double.parseDouble(tokens[1]);
                    pqueue.add(article, score);
                } catch (NumberFormatException e) {
                    System.err.println("invalid value: " + t);
                } catch (ArrayIndexOutOfBoundsException e) {
                    System.err.println("invalid value: " + t);
                }
            }

            StringBuilder builder = new StringBuilder();
            int flag = 0;
            for (WordScore ws : pqueue) {
                if(flag > 0){
                  builder.append("|");
                }
                builder.append(ws.word);
                builder.append(",");
                builder.append(Utils.truncateDouble("" + ws.score, 8));
                flag ++;
            }
            pqueue.clear();

            context.write(new Text(key), new Text(builder.toString()));
            builder.setLength(0);
        }
    }

    /**
     * Runs this tool.
     */
    public int run(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("usage: input output");
            ToolRunner.printGenericCommandUsage(System.out);
            return -1;
        }

        Path inputPath = new Path(args[0]);
        Path outputPath = new Path(args[1]);

        Configuration conf = getConf();

        Job job = new Job(conf, this.getClass().toString());
        FileInputFormat.setInputPaths(job, inputPath);
        FileOutputFormat.setOutputPath(job, outputPath);

        FileOutputFormat.setCompressOutput(job, true);

        job.setJarByClass(Step6DocSimFormatter.class);
        job.setInputFormatClass(KeyValueTextInputFormat.class);
        job.setOutputFormatClass(TextOutputFormat.class);
        job.setMapOutputKeyClass(Text.class);
        job.setMapOutputValueClass(Text.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(Text.class);

        job.setMapperClass(MyMapper.class);
        job.setReducerClass(MyReducer.class);
        FileSystem hdfs = FileSystem.get(outputPath.toUri(), conf);
        if (hdfs.exists(outputPath)) {
            hdfs.delete(outputPath, true);
        }

        return job.waitForCompletion(true) ? 0 : 1;
    }

    /**
     * Dispatches command-line arguments to the tool via the
     * <code>ToolRunner</code>.
     */
    public static void main(String[] args) throws Exception {
        int res = ToolRunner.run(new Configuration(), new Step6DocSimFormatter(), args);
        System.exit(res);
        return;
    }

}

