// https://searchcode.com/api/result/134046543/

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package wmr.tfidf;

import java.io.IOException;
import java.util.TreeSet;
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


/**
 * Fourth stage of similarity calculation.
 * Calculates the similarities between two documents.
 *
 * I run this as follows:
 *
 * hadoop jar ./wikiMiner-deps.jar edu.macademia.wikiminer.DocSimScorer \
 *          -D mapred.map.tasks=50
 *          -D mapred.reduce.tasks=50 \
 *          /user/shilad/macademia/res/3 \
 *          /user/shilad/macademia/res/4
 *
 * Compression is crucial for this job because output size is the bottleneck.
 * It's also crucial that the compression codec be LzoCodec, because the input
 * must be splittable and Gzip is not.
 *
 * @author shilad
 */
public class Step4DocSimScorer extends Configured implements Tool {
    private static final String KEY_MAX_DOCS_PER_TERM = "MAX_DOCS";

    private static int MAX_DOCS_PER_TERM = 2000;

    private static class MyMapper extends Mapper<Text, Text, Text, Text> {
        @Override
        public void map(Text key, Text value, Context context) throws IOException, InterruptedException {
            String article = key.toString();
            String [] tokens = value.toString().split("@");
            if (tokens.length == 2) {
                String word = tokens[0];
                String score = tokens[1];
                context.write(new Text(word), new Text(article + "@" + score));
            } else {
                System.err.println("invalid key/value pair: " + key + ", " + value);
            }
        }
    }

    private static class MyReducer extends Reducer<Text, Text, Text, Text> {
        private int maxDocsPerTerm = -1;

        @Override
        public void setup(Context context) {
            maxDocsPerTerm = context.getConfiguration().getInt(KEY_MAX_DOCS_PER_TERM, -1);
            if (maxDocsPerTerm < 0) {
                throw new IllegalArgumentException("no maxDocsPerTerm arg (should be impossible)");
            }
            System.err.println("maxDocsPerTerm IS " + maxDocsPerTerm);
        }

        @Override
        public void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
            //String word = key.toString();
            // Reducer takes each word in the bins (splitted words) and returns a pair of words and their tf-idf score
            // First place them into a list...
            // The "word" in word score is actually an article name.
            TreeSet<WordScore> wordScores = new TreeSet<WordScore>();
            for (Text t : values) {
                String [] tokens = t.toString().split("@");
                String article = tokens[0];
                double score = Double.parseDouble(tokens[1]);
                WordScore ws = new WordScore(article, score);
                if (wordScores.size() < MAX_DOCS_PER_TERM) {
                    wordScores.add(ws);
                } else if (ws.compareTo(wordScores.last()) < 0) {
                    wordScores.remove(wordScores.last());
                    wordScores.add(ws);
                }
            }

            for (WordScore ws1 : wordScores) {
                for (WordScore ws2 : wordScores) {
                    double s = ws1.score * ws2.score;
                    context.write(new Text(ws1.word + "@" + ws2.word), new Text(""+s));
                }
            }
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
        conf.setInt(KEY_MAX_DOCS_PER_TERM,
                args.length >= 3 ? Integer.valueOf(args[2]) : MAX_DOCS_PER_TERM);

        Job job = new Job(conf, this.getClass().toString());
        FileInputFormat.setInputPaths(job, inputPath);
        FileOutputFormat.setOutputPath(job, outputPath);

        job.setJarByClass(Step4DocSimScorer.class);
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
        int res = ToolRunner.run(new Configuration(), new Step4DocSimScorer(), args);
        System.exit(res);
        return;
    }
}


