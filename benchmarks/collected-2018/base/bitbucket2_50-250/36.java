// https://searchcode.com/api/result/134046528/

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package wmr.citations;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.*;
import org.apache.hadoop.mapreduce.Reducer.Context;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import wmr.core.AllRevisions;
import wmr.core.AllRevisionsInputFormat;
import wmr.core.Page;
import wmr.core.Revision;

/**
 *@author Shilad Sen
 */
public class CitationReplacements extends Configured implements Tool {

    /*
     * Reads output of CitationCounter
     */
    public static class MyMapper extends Mapper<Long, AllRevisions, Text, Text> {
        private static final int CONTEXT_LENGTH = 30;

        @Override
        public void setup(Mapper.Context context) {
        }

        @Override
        public void map(Long key, AllRevisions allRevs, Mapper.Context mcontext) throws IOException, InterruptedException {
            mcontext.progress();
            if (!allRevs.getPage().isNormalPage()) {
                return;
            }
            Map<String, String> prevContextAndCites = new HashMap<String, String>();
            for (Revision rev : allRevs.getRevisions()) {
                mcontext.progress();
                try {
                    Map<String, String> contextAndCites = getContextAndCites(allRevs.getPage(), rev);
                    if (!rev.getContributor().isBot()) {
                        for (Map.Entry<String, String> entry : contextAndCites.entrySet()) {
                            String context = entry.getKey();
                            String domain = entry.getValue();
                            String oldDomain = prevContextAndCites.get(context);
                            if (domain != null && oldDomain != null && !oldDomain.equals(domain)) {
                                mcontext.write(
                                        new Text(key + "@" + allRevs.getPage().getName()),
                                        new Text("" + rev.getId() + "\t" + rev.getTimestamp() + "\t" + oldDomain + "\t" + domain));
                            }
                        }
                    }
                    prevContextAndCites = contextAndCites;
                } catch (Exception e) {
                    System.err.println("page " + allRevs.getPage().getName() + ", rev " + rev.getTimestamp() + " failed:");
                    e.printStackTrace();
                }
            }
        }

        private Map<String, String> getContextAndCites(Page page, Revision rev) {
            Map<String, String> contextAndCites = new HashMap<String, String>();
            for (Citation c : rev.getCitations(page)) {
                // get context
                String context = rev.getText().substring(0, c.getLocation());
                if (context.length() > CONTEXT_LENGTH) {
                    context = context.substring(0, CONTEXT_LENGTH);
                }
                if (contextAndCites.containsKey(context)) {
                    contextAndCites.put(context, null);     // mark it as invalid
                } else {
                    contextAndCites.put(context, c.getUrlDomain());
                }
            }
            return contextAndCites;
        }
    }
    
    public static class MyReducer extends Reducer<Text,Text,Text,Text> {

        @Override
        protected void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
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

        
        job.setJarByClass(CitationReplacements.class);
        job.setInputFormatClass(AllRevisionsInputFormat.class);
        job.setOutputFormatClass(TextOutputFormat.class);
        job.setMapOutputKeyClass(Text.class);
        job.setMapOutputValueClass(Text.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(Text.class);
        
        job.setMapperClass(MyMapper.class);
//        job.setReducerClass(MyReducer.class);
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
        int res = ToolRunner.run(new CitationReplacements(), args);
        System.exit(res);
    }
}

