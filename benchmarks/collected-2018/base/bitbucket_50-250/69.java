// https://searchcode.com/api/result/134046545/

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package wmr.tfidf;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import wikiParser.mapReduce.util.KeyValueTextInputFormat;


/**
 * <p>
 * Third stage in processing.
 * Given a vector of words representing the Tf-Idf scores for an article,
 * prunes it down to a small set of highly representative words.
 * </p>
 *
 * <p>
 * The command line of this requires the total number of wikipedia pages
 * as an argument.  As of April 2010, this was about 1.8 million.
 * 
 * Example invocation:
 * </p>
 *
 * <pre>
 * hadoop jar ./wikiMiner-deps.jar edu.macademia.wikiminer.TfidfVectorPruner \
 *                  -D mapred.map.tasks=50 \
 *                  -D mapred.reduce.tasks=50 \
 *                  /user/shilad/macademia/res/2/ \
 *                  /user/shilad/macademia/res/3 \
 *                  2000000 \
 *                  100
 * </pre>
 *
 *
 * @author Shilad Sen
 */
public class Step3TfidfVectorPruner extends Configured implements Tool {

    private static final String KEY_NUM_DOCUMENTS = "NUM_DOCUMENTS";
    private static final String KEY_SMOOTHING = "SMOOTHING";
    
    private static int NUM_DOCUMENTS = 668;
    private static int MAX_DOCUMENTS_FOR_TERM = NUM_DOCUMENTS / 20;
    private static int SMOOTHING_CONSTANT = 100;

    private static final int MAX_VECTOR_SIZE = 600;
    private static final int MIN_DOCUMENTS_FOR_TERM = 3;

    
    /**
     * <p>
     * Input is:
     * "Anarchism   developed@345@6249@32424"
     * Key is article.
     * Value is word@articleWordFrequency@articleWordCount@wordDocFrequency
     * </p>
     *
     * <p>
     * Output is:
     * "Anarchism   developed@32.0"
     * </p>
     */
    private static class MyReducer extends Reducer<Text, Text, Text, Text> {
        
        @Override
        public void setup(Context context) {
            NUM_DOCUMENTS = context.getConfiguration().getInt(KEY_NUM_DOCUMENTS, 689);
            MAX_DOCUMENTS_FOR_TERM = NUM_DOCUMENTS / 20;
            SMOOTHING_CONSTANT = context.getConfiguration().getInt(KEY_NUM_DOCUMENTS, 689);
            System.out.println("MAX_DOCs for term = " + MAX_DOCUMENTS_FOR_TERM);
            System.out.println("SMOOTHING_CONSTANT for term = " + SMOOTHING_CONSTANT);
        }

        @Override
        public void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
            String article = key.toString();

            List<WordScore> scores = new ArrayList<WordScore>();
            for (Text t : values) {
                String value = t.toString();
                try {
                    String [] tokens = value.split("@");
                    String word = tokens[0];
                    int termCount = Integer.parseInt(tokens[1]);
                    int docLength = Integer.parseInt(tokens[2]);
                    int docCount = Integer.parseInt(tokens[3]);

                    if (docCount > MAX_DOCUMENTS_FOR_TERM || docCount < MIN_DOCUMENTS_FOR_TERM) {
                        continue;
                    }

                    double tf = 1.0 * termCount / (docLength + SMOOTHING_CONSTANT);
                    double idf = Math.log(1.0 * NUM_DOCUMENTS / docCount);

                    WordScore score = new WordScore(word, Math.sqrt(tf*idf));
                    scores.add(score);
                } catch (NumberFormatException e) {
                    System.err.println("invalid value: " + value);
                } catch (IndexOutOfBoundsException e) {
                    System.err.println("invalid value: " + value);
                }
            }
            Collections.sort(scores);
            double length = 0.0;
            for (int i = 0; i < scores.size() && i < MAX_VECTOR_SIZE; i++) {
                double s = scores.get(i).score;
                length += s*s;
            }
            length = Math.sqrt(length);
            if (scores.size() < MAX_VECTOR_SIZE) {
                length *= Math.sqrt(1.0 * MAX_VECTOR_SIZE / scores.size());
            }
            
            for (int i = 0; i < scores.size() && i < MAX_VECTOR_SIZE; i++) {
                WordScore s = scores.get(i);
                context.write(key, new Text("" + s.word + "@" + (s.score / length)));
            }
        }
        
    }


    /**
     * Runs this tool.
     */
    public int run(String[] args) throws Exception {
        if (args.length < 3) {
            System.out.println("usage: [input output numdocuments {smoothing_constant}]");
            ToolRunner.printGenericCommandUsage(System.out);
            return -1;
        }

        Path inputPath = new Path(args[0]);
        Path outputPath = new Path(args[1]);

        Configuration conf = getConf();
        conf.setInt(KEY_NUM_DOCUMENTS, Integer.valueOf(args[2]));
        conf.setInt(KEY_SMOOTHING, (args.length >= 4) ? Integer.valueOf(args[2]) : SMOOTHING_CONSTANT);

        Job job = new Job(conf, this.getClass().toString());
        FileInputFormat.setInputPaths(job, inputPath);
        FileOutputFormat.setOutputPath(job, outputPath);

        job.setJarByClass(Step3TfidfVectorPruner.class);
        job.setInputFormatClass(KeyValueTextInputFormat.class);
        job.setOutputFormatClass(TextOutputFormat.class);
        job.setMapOutputKeyClass(Text.class);
        job.setMapOutputValueClass(Text.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(Text.class);

        job.setReducerClass(MyReducer.class); // identity reducer
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
        int res = ToolRunner.run(new Configuration(), new Step3TfidfVectorPruner(), args);
        System.exit(res);
        return;
    }
}


