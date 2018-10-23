// https://searchcode.com/api/result/134046548/

package wmr.tfidf;

import gnu.trove.set.hash.TIntHashSet;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.*;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.wikipedia.miner.util.MarkupStripper;

import wmr.core.*;
import wmr.util.EasyLineReader;
import wmr.util.Utils;

/**
 * @author Shilad Sen
 */
public class Step1PageWords extends Configured implements Tool {

    private static final Logger LOG  = Logger.getLogger(Step1PageWords.class.getPackage().getName());
    private static final String KEY_ID_FILTER = "ID_FILTER";

    public static final int MIN_DOCUMENT_LENGTH = 100;

    public static class MyMapper extends Mapper<Long, CurrentRevision, Text, Text> {

        TIntHashSet filter = null;

        @Override
        public void setup(Mapper.Context context) throws IOException {
            String path = context.getConfiguration().get(KEY_ID_FILTER);
            LOG.log(Level.INFO, "filter path was {0}", path);
            if (path != null) {
                filter = new TIntHashSet();
                EasyLineReader reader = new EasyLineReader(new Path(path), context.getConfiguration());
                for (String line : reader) {
                    try {
                        filter.add(Integer.valueOf(line.split("\\s+")[0]));
                    } catch (NumberFormatException e) {
                        // ignore it....
                    }
                }
                LOG.log(Level.INFO, "Read {0} pages into filter", filter.size());
            }
        }

        @Override
        public void map(Long key, CurrentRevision value, Mapper.Context context)
                throws IOException, InterruptedException {
            try {
                context.progress();
                Page p = value.getPage();
                Revision r = value.getRevision();
                if (!shouldProcessPage(p, r)) {
                    return;
                }
                String text = MarkupStripper.stripEverything(r.getText());
                Pattern pattern = Pattern.compile("\\w+");
                Matcher m = pattern.matcher(text);
                int total = 0;
                Map<String, Integer> counts = new HashMap<String, Integer>();
                while (m.find()) {
                    String word = m.group().toLowerCase();
                    if (Utils.STOP_WORDS.contains(word)) {
                        continue;
                    }
                    word = Utils.stem(word);
                    total++;
                    if (!counts.containsKey(word)) {
                        counts.put(word, 1);
                    } else {
                        counts.put(word, counts.get(word) + 1);
                    }
                }
                if (total <= MIN_DOCUMENT_LENGTH) {
                    return;
                }

                StringBuilder builder = new StringBuilder();
                for (String word : counts.keySet()) {
                    builder.append("@");
                    builder.append(p.getId());
                    builder.append("@");
                    builder.append("" + counts.get(word));
                    builder.append("@");
                    builder.append("" + total);
                    context.write(
                            new Text(word),
                            new Text(builder.toString()));
                    builder.setLength(0);

                 }
            } catch (Exception e) {
                System.err.println("processing of " + key + " failed:");
                e.printStackTrace();
            }
        }

        private boolean shouldProcessPage(Page p, Revision r) {
            if (!p.isNormalPage() || r.isDisambiguation() || r.isRedirect()) {
                return false;
            }
            if (filter != null && !filter.contains(p.getIdAsInt())) {
                return false;
            }
            return true;
        }
    }


    /**
     * Prepends a word#c containing the total count for the word across all
     * wikipedia articles.
     */
    public static class MyReducer extends Reducer<Text, Text, Text, Text> {

        @Override
        protected void reduce(Text key, Iterable<Text> values, Context context
                        ) throws IOException, InterruptedException {
            int documentCount = 0;
            for (Text value : values) {
                context.write(key, value);
                documentCount++;
                if (documentCount % 10000 == 0) {
                    context.progress();
                }
            }
            context.write(new Text(key.toString() + "#c"), new Text(""+documentCount));
        }
    }


    public int run(String args[]) throws Exception {

        if (args.length < 2) {
            System.out.println("usage: [input output {id_filter}]");
            ToolRunner.printGenericCommandUsage(System.out);
            return -1;
        }

        Path inputPath = new Path(args[0]);
        Path outputPath = new Path(args[1]);

        Configuration conf = getConf();
        System.err.println("length of args is " + args.length);
        if (args.length >= 3) {
            System.err.println("SETTING FILTER!");
            conf.set(KEY_ID_FILTER, args[2]);
        }
        
        Job job = new Job(conf, this.getClass().toString());

        FileInputFormat.setInputPaths(job, inputPath);
        FileOutputFormat.setOutputPath(job, outputPath);

        
        job.setJarByClass(Step1PageWords.class);
        job.setInputFormatClass(CurrentRevisionInputFormat.class);
        job.setOutputFormatClass(TextOutputFormat.class);
        job.setMapOutputKeyClass(Text.class);
        job.setMapOutputValueClass(Text.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(Text.class);
        
        job.setMapperClass(MyMapper.class);
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
        int res = ToolRunner.run(new Step1PageWords(), args);
        System.exit(res);
    }
}

