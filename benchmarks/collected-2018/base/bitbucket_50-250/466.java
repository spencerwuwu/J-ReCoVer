// https://searchcode.com/api/result/134046546/

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
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
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import wikiParser.mapReduce.util.KeyValueTextInputFormat;
import wmr.util.SecondarySortOnHash;


/**
 * <p>
 * The second stage of the document similarity calculation.
 * Calculates Tf-Idf values for every word in every article.
 * </p>
 *
 * <p>Sample invocation:</p>
 * <pre>
 * 
 * hadoop jar ./wikiMiner-deps.jar edu.macademia.wikiminer.DocFrequency \
 *              -D mapred.map.tasks=100 \
 *              -D mapred.reduce.tasks=100 \
 *              -D mapred.output.compress=true \
 *              /user/shilad/macademia/res/1/ \
 *              /user/shilad/macademia/res/2/
 * </pre>
 * 
 * @author shilad
 */
public class Step2DocFrequency extends Configured implements Tool {
    /**
     *
     * <p>
     * Input:
     * Word document count: "developed#c 32424"
     * Followed by lines like: "developed   @Anarchism@5@6249"
     * key is word and value is article name, count, article word count
     * </p>
     *
     * <p>
     * Ouput is:
     * "Anarchism   developed@345@6249@32424"
     * Key is article.
     * Value is word@articleWordFrequency@articleWordCount@wordDocFrequency
     * </p>
     *
     */
    private static class MyReducer extends Reducer<Text, Text, Text, Text> {
        private String lastWord = null;
        private int docFrequency = -1;

        @Override
        protected void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
            String word = key.toString();
            int i = word.indexOf("#");
            if (i >= 0) {
                Iterator<Text> iter = values.iterator();
                if (!iter.hasNext()) {
                    throw new IllegalStateException("key " + word + " didn't have a count");
                }
                lastWord = word.substring(0, i);
                docFrequency = Integer.parseInt(iter.next().toString());
                return;
            }
            if (!word.equals(lastWord)) {
                throw new IllegalStateException("received " + word + " when expecting " + lastWord);
            }
            StringBuilder builder = new StringBuilder();
            for (Text t : values) {
                String value = t.toString();
                String [] tokens = value.split("@");
                if (tokens.length >= 4) {
                    String article = tokens[1];
                    String count = tokens[2];
                    String docLength = tokens[3];
                    builder.append(word);
                    builder.append("@");
                    builder.append(count);
                    builder.append("@");
                    builder.append(docLength);
                    builder.append("@");
                    builder.append(docFrequency);
                    context.write(new Text(article), new Text(builder.toString()));
                    builder.setLength(0);
                }
            }
        }
    }


    public int run(String args[]) throws Exception {
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

        job.setJarByClass(Step2DocFrequency.class);
        job.setInputFormatClass(KeyValueTextInputFormat.class);
        job.setOutputFormatClass(TextOutputFormat.class);
        job.setMapOutputKeyClass(Text.class);
        job.setMapOutputValueClass(Text.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(Text.class);
        job.setReducerClass(MyReducer.class);
        SecondarySortOnHash.setupSecondarySortOnHash(job);

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
        int res = ToolRunner.run(new Step2DocFrequency(), args);
        System.exit(res);
    }
}


