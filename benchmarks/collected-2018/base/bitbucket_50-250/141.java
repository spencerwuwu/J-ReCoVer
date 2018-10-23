// https://searchcode.com/api/result/134047289/

package wikiParser.mapReduce.graphs;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;

import java.util.List;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import wmr.core.Page;

import wmr.core.PageParser;
import wmr.core.Edge;
import wmr.core.Revision;
import wikiParser.edges.ArticleArticleGenerator;
import wikiParser.mapReduce.util.KeyValueTextInputFormat;
import wmr.util.LzmaDecompresser;
import wmr.util.Utils;
/**
 * Creates Article to Article graph with directed edges using links only
 * @author Nathaniel Miller
 */
public class InitialArticleLinkMapReduce extends Configured implements Tool {
    /*
     * Takes key-value 7zip hashes and outputs ID-links pairs.
     */
    public static class MyMap extends Mapper<Text,Text,Text,Text> {

        @Override
        public void map(Text key, Text value, Mapper.Context context) throws IOException {
            
            /*
             * Input: ArticleID-7zipHash key-value pairs.
             * Output: ArticleID-Edge key-value pairs with a 2-digit connection type demarcation.
             * 1. Unzip value
             * 2. Get page info
             * 3. Build connection list
             * 4. Find links
             * 5. Emit individual ID-link pairs with connection type markers.
             */
            LzmaDecompresser pipe = null;
            try {
                context.progress();
                int length = Utils.unescapeInPlace(value.getBytes(), value.getLength());
                pipe = new LzmaDecompresser(value.getBytes(), length);
                PageParser parser = new PageParser(pipe.decompress());
                Page article = parser.getArticle();
                ArticleArticleGenerator edgeGenerator = new ArticleArticleGenerator();
                Revision rev = null;
                while (true) {
                    Revision next = parser.getNextRevision();
                    context.progress();
                    if (next == null) {
                        break;
                    }
                    rev = next;
                }
                if (rev != null) {
                    List<Edge> edges = edgeGenerator.generateWeighted(article,rev);
                    if (edges != null) {
                        for (Edge link : edgeGenerator.generateWeighted(article, rev)) {
                            if (article.isUserTalk() || article.isUser()) {
                                context.write(new Text("u" + article.getUser().getId()), new Text(link.toOutputString()));
                            } else {
                                context.write(new Text("a" + article.getId()), new Text(link.toOutputString()));
                            }
                        }
                    }
                } 
            } catch (Exception e) {
                System.err.println("error when processing " + key + ":");
                e.printStackTrace();
            } finally {
                if (pipe != null) {
                    pipe.cleanup();
                }
            }
        }
    }

    public static class MyReduce extends Reducer<Text,Text,Text,Text> {

        @Override
        public void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
            Iterator<Text> iterator = values.iterator();
            HashMap<String,String> edges = new HashMap<String, String>();
            while (iterator.hasNext()) {
                String v = iterator.next().toString();
                String[] a = v.split("\\|");
                if (a.length > 2) {
                    String k = a[2];
                    if (!edges.containsKey(k)) {
                        edges.put(k,v);
                    } else {
                        String[] split = edges.get(k).split("\\|");
                        edges.put(k, split[0] + "|" + Math.max(Integer.parseInt(split[1]), Integer.parseInt(v.split("\\|")[1])) + "|" + k);
                    }
                } else {
                    System.err.println("BAD VALUE - key: " + key.toString() + ", value: '" + v + "'");
                }
            }
            StringBuilder result = new StringBuilder();
            for (String v  : edges.values()) {
                result.append(v).append(" ");
            }
            context.write(key, new Text(result.toString()));
        }
        
    }

        
    @Override
    public int run(String args[]) throws Exception {

        if (args.length < 2) {
            System.out.println("usage: [input output]");
            ToolRunner.printGenericCommandUsage(System.out);
            return -1;
        }

        Path inputPath = new Path(args[0]);
        Path outputPath = new Path(args[1]);

        Configuration conf = getConf();
        Job job = new Job(conf, this.getClass().toString());

        FileInputFormat.setInputPaths(job, inputPath);
        FileOutputFormat.setOutputPath(job, outputPath);

        
        job.setJarByClass(InitialArticleLinkMapReduce.class);
        job.setInputFormatClass(KeyValueTextInputFormat.class);
        job.setOutputFormatClass(TextOutputFormat.class);
        job.setMapOutputKeyClass(Text.class);
        job.setMapOutputValueClass(Text.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(Text.class);
        
        job.setMapperClass(MyMap.class);
        job.setReducerClass(MyReduce.class);
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
        int res = ToolRunner.run(new InitialArticleLinkMapReduce(), args);
        System.exit(res);
    }
}

