// https://searchcode.com/api/result/74993295/

package edu.uiowa.icts.hadoop;

import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.ling.Sentence;
import edu.stanford.nlp.ling.TaggedWord;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.*;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.jsoup.Jsoup;

/**
 * Part of Speech Counting.
 *
 */
public class POSCount extends Configured implements Tool {
    static class POSCountMapper extends MapReduceBase implements Mapper<LongWritable, Text, Text, LongWritable> {
        MaxentTagger tagger = null;
        private static LongWritable ONE = new LongWritable(1L);

        @Override
        public void configure(JobConf job) {
            try {
                tagger = new MaxentTagger("models/wsj-0-18-left3words.tagger");
            } catch (Exception e) {
                System.out.println("Cannot load the model for tagger.");
            }
        }

        public void map(LongWritable key, Text value, OutputCollector<Text, LongWritable> output, Reporter reporter) throws IOException {
            // convert value to LongWritable
            //long val = Long.parseLong(value.toString());
            //output.collect(new LongWritable(val), new Text(""));
            //context.wri
            try {
                // analyze one line
                String[] params = value.toString().split(",", 6);
                System.out.println(params[5]);
                String text = params[5];
                // remove the first the last "" quotes.
                text = text.substring(1, text.length() - 1);
                text = Jsoup.parse(text).text();


                // create an empty Annotation just with the given text
                @SuppressWarnings("unchecked")
                List<List<HasWord>> sentences = tagger.tokenizeText(new StringReader(text));
                for (List<HasWord> sentence : sentences) {
                    ArrayList<TaggedWord> tSentence = tagger.tagSentence(sentence);
                    // System.out.println(Sentence.listToString(tSentence, false));
                    for (TaggedWord tag: tSentence) {
                        output.collect(new Text(tag.tag()), ONE);
                    }
                }
            } catch (Exception e) {
                System.out.println(e);
            }
            //output.collect(new Text(t.toString()), );

        }

    }

    static class POSCountReducer extends MapReduceBase implements Reducer<Text, LongWritable, Text, LongWritable> {

        public void reduce(Text key, Iterator<LongWritable> values, OutputCollector<Text, LongWritable> output, Reporter reporter)
                throws IOException {
            long total = 0;
            while (values.hasNext()) {
                total += values.next().get();
                //output.collect(key, values.next());
            }
            output.collect(key, new LongWritable(total));
        }
    }

    @Override
    public int run(String[] args) throws Exception {
        JobConf conf = new JobConf(POSCount.class);
        conf.setJobName("POS Count");
        conf.set("mapred.child.java.opts", "-Xmx1024m");

        List<String> other_args = new ArrayList<String>();
        for (int i = 0; i < args.length; ++i) {
            try {
                if ("-m".equals(args[i])) {
                    conf.setNumMapTasks(Integer.parseInt(args[++i]));
                } else if ("-r".equals(args[i])) {
                    conf.setNumReduceTasks(Integer.parseInt(args[++i]));
                } else {
                    other_args.add(args[i]);
                }
            } catch (NumberFormatException except) {
                System.out.println("ERROR: Integer expected instead of " + args[i]);
                return printUsage();
            } catch (ArrayIndexOutOfBoundsException except) {
                System.out.println("ERROR: Required parameter missing from "
                        + args[i - 1]);
                return printUsage();
            }
        }
        if (other_args.size() != 2) {
            System.out.println("ERROR: Wrong number of parameters: "
                    + other_args.size() + " instead of 2.");
            return printUsage();
        }

        FileInputFormat.addInputPath(conf, new Path(other_args.get(0)));
        FileOutputFormat.setOutputPath(conf, new Path(other_args.get(1)));

        conf.setMapperClass(POSCountMapper.class);
        conf.setReducerClass(POSCountReducer.class);

        // This line is important, otherwise you may encounter mismatch type error.
        conf.setMapOutputKeyClass(Text.class);
        conf.setMapOutputValueClass(LongWritable.class);

        conf.setOutputKeyClass(LongWritable.class);
        conf.setOutputValueClass(Text.class);

        //System.exit(conf.waitForCompletion(true)? 0 : 1);
        JobClient.runJob(conf);
        return 0;
    }

    static int printUsage() {
        System.out.println("POScount [-m <maps>] [-r <reduces>] <input> <output>");
        ToolRunner.printGenericCommandUsage(System.out);
        return -1;
    }

    public static void main(String[] args) throws Exception {
        int exitCode = ToolRunner.run(new POSCount(), args);
        System.exit(exitCode);
    }
}
