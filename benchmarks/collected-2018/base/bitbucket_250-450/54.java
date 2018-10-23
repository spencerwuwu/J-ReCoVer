// https://searchcode.com/api/result/134046549/

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package wmr.tfidf;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.*;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.FileSplit;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import wikiParser.mapReduce.util.KeyValueTextInputFormat;
import wmr.util.Utils;


/**
 * @author Shilad Sen
 */
public class Step8FinalResultCombiner extends Configured implements Tool {

    private static final Logger LOG  = Logger.getLogger(Step8FinalResultCombiner.class.getPackage().getName());
    
    private static final char TYPE_LINKS = 'l';
    private static final char TYPE_WORDS = 'w';
    private static final char TYPE_CATS = 'c';
    private static final boolean DEBUG = false;

    public static class MyMapper extends Mapper<Text, Text, Text, Text> {

        private char featureType;

        @Override
        public void setup(Mapper.Context context) throws IOException {
            String path = ((FileSplit)context.getInputSplit()).getPath().toString();
            assert(path != null);
            LOG.log(Level.INFO, "input path for mapper was {0}", path);
            if (path.contains("links")) {
                featureType = TYPE_LINKS;
            } else if (path.contains("words")) {
                featureType = TYPE_WORDS;
            } else if (path.contains("categories")) {
                featureType = TYPE_CATS;
            } else {
                throw new IllegalArgumentException("path doesnt match a feature: '" + path + "'");
            }
        }

        @Override
        public void map(Text key, Text value, Context context)
                throws IOException, InterruptedException {
            try {
                String skey = stripQuotes(key.toString());
                String sval = stripQuotes(value.toString());
                context.progress();
                context.write(new Text(skey), new Text("" + featureType + sval));
            } catch (Exception e) {
                System.err.println("processing of " + key + " failed:");
                e.printStackTrace();
            }
        }
    }

    public static class MyReducer extends Reducer<Text, Text, Text, Text> {
        @Override
        public void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
            context.progress();

            // collect
            Set<Character> featureTypes = new TreeSet<Character>();
            Map<Integer, List<SimPage>> allSims = new HashMap<Integer, List<SimPage>>();
            for (Text value : values) {
                context.progress();
                String line = value.toString();
                char ft = line.charAt(0);
                if (ft != TYPE_LINKS && ft != TYPE_WORDS && ft != TYPE_CATS) {
                    throw new IllegalArgumentException("invalid feature type: " + ft);
                }
                featureTypes.add(ft);
                for (SimPage sp : parseSims(line)) {
                    if (!allSims.containsKey(sp.pageId)) {
                        allSims.put(sp.pageId, new ArrayList<SimPage>());
                    }
                    allSims.get(sp.pageId).add(sp);
                }
            }

            // calculate score
            Map<String, String> pageFeatures = new HashMap<String, String>();
            List<WordScore> finalScores = new ArrayList<WordScore>();
            for (Integer pageId : allSims.keySet()) {
                List<SimPage> sps = allSims.get(pageId);
                double score = combineScores(featureTypes, sps);
                if (!Double.isNaN(score)) {
                    finalScores.add(new WordScore("" + pageId, score));
                    
                    // for debugging purposes
                    if (DEBUG) {
                        String pfs = "";
                        for (SimPage sp : sps) {
                            pfs += (char)sp.featureType;
                        }
                        pageFeatures.put("" + pageId, pfs);
                    }
                }
                
            }
            Collections.sort(finalScores);
            context.progress();

            // prepare and write output
            StringBuilder buff = new StringBuilder();

            for (char c : featureTypes) { buff.append(c); };
            buff.append("\t");

            for (int i = 0; i < Math.min(10000, finalScores.size()); i++) {
                WordScore ws = finalScores.get(i);
                if (i > 0) {
                    buff.append('|');
                }
                buff.append(ws.word).append(',').append(Utils.truncateDouble("" + ws.score, 6));
                if (DEBUG) {
                    buff.append(",");
                    buff.append(pageFeatures.get(ws.word));
                }
            }
            context.write(key, new Text(buff.toString()));
        }

        private double combineScores(Set<Character> featureTypes, List<SimPage> sps) {
            // if all features available, but only cat scored this page.
            if (featureTypes.size() == 3 && sps.size() == 1 && sps.get(0).featureType == TYPE_CATS) {
                return Double.NaN;  // unreliable!
            }
            double linkScore = -0.05;
            double catScore = -0.01;
            double wordScore = -0.05;

            for (SimPage sp : sps) {
                if (sp.featureType == TYPE_WORDS) {
                    wordScore = sp.similarity;
                } else if (sp.featureType == TYPE_LINKS) {
                    linkScore = sp.similarity;
                } else {
                    assert(sp.featureType == TYPE_CATS);
                    catScore = 1.0 - sp.rank / 20000.0;
                }
            }

            return 5.84921 + 5.45869 * wordScore + 1.48050 * linkScore + 0.90944 * catScore;
        }
        
        private List<SimPage> parseSims(String line) {
            char featureType = line.charAt(0);
            if (featureType != TYPE_LINKS && featureType != TYPE_WORDS && featureType != TYPE_CATS) {
                throw new IllegalArgumentException("invalid feature type: " + featureType);
            }
            line = stripQuotes(line.substring(1));
            List<SimPage> sims = new ArrayList<SimPage>();
            for (String pair : line.split("\\|")) {
                String tokens[] = pair.split(",");
                if (tokens.length == 2) {
                    sims.add(new SimPage(featureType, Integer.valueOf(tokens[0]), sims.size(), Float.valueOf(tokens[1])));
                } else {
                    LOG.log(Level.WARNING, "invalid token in similarity: {0}", pair);
                }
            }
            return sims;
        }
    }

    public static class SimPage {
        char featureType;
        int pageId;
        int rank;
        float similarity;

        public SimPage(char featureType, int pageId, int rank, float similarity) {
            this.featureType = featureType;
            this.pageId = pageId;
            this.rank = rank;
            this.similarity = similarity;
        }
    }

    private static String stripQuotes(String s) {
        if (s.startsWith("\"")) {
            s = s.substring(1);
        }
        if (s.endsWith("\"")) {
            s = s.substring(0, s.length() - 1);
        }
        return s;
    }


    public int run(String args[]) throws Exception {
        if (args.length < 2) {
            System.out.println("usage: input output");
            ToolRunner.printGenericCommandUsage(System.out);
            return -1;
        }

        Configuration conf = getConf();
        Job job = new Job(conf, this.getClass().toString());
        for (int i = 0; i < args.length-1; i++) {
            FileInputFormat.addInputPath(job, new Path(args[i]));
        }

        Path outputPath = new Path(args[args.length-1]);
        FileOutputFormat.setOutputPath(job, outputPath);
        
        job.setJarByClass(Step8FinalResultCombiner.class);
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
        int res = ToolRunner.run(new Step8FinalResultCombiner(), args);
        System.exit(res);
    }
}

