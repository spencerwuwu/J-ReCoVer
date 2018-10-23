// https://searchcode.com/api/result/134047311/

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package wikiParser.userCharacterization;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import javax.xml.stream.XMLStreamException;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.filecache.DistributedCache;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import wikiParser.mapReduce.util.KeyValueTextInputFormat;
import wmr.core.Page;
import wmr.core.PageParser;
import wmr.core.Revision;
import wmr.util.LzmaDecompresser;
import wmr.util.Utils;

/**
 *
 * @author Nathaniel
 * 
 * Output: clusterID    userID#deltaBytes|userID1#deltaBytes1|...
 */
public class DeltaByteCounter extends Configured implements Tool {

    
    //Use JobConf/DistributedCache to get needed information to Mapper/Reducer
    //What I/O do we want
    public static class PageMapper extends Mapper<Text,Text,IntWritable,Text> {
       // HashMap<String,Integer> aToCMap; //article to cluster mapping
        
        /**
         * This method gets information about the article to cluster mapping
         * from the distributed cache.  DistributedCache has not been updated
         * to the 0.20 API, so this is a little quirky right now, but I think 
         * it should work.
         * @param context
         * @throws IOException 
         */
        /*@Override
        protected void setup(Context context) throws IOException {
            Path[] localCache = DistributedCache.getLocalCacheFiles(context.getConfiguration());
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(new File(localCache[0].toUri()))));
            aToCMap = new HashMap<String,Integer>();
            String line = reader.readLine();
            int cluster = 0;
            while (line != null) {
                for (String name : line.split("\\|")) {
                    aToCMap.put(name, cluster);
                }
                line = reader.readLine();
                cluster++;
            }
        }*/
        
        @Override
        public void map(Text key, Text value, Mapper.Context context) throws IOException, InterruptedException {
            LzmaDecompresser pipe = null;
            try {
                context.progress();
                int size = Utils.unescapeInPlace(value.getBytes(), value.getLength());
                pipe = new LzmaDecompresser(value.getBytes(), size);
                PageParser parser = new PageParser(pipe.decompress());
                Page article = parser.getArticle();
                if (article.isNormalPage()) {
                    HashMap<String,Integer> delta = new HashMap<String,Integer>();
                    //int clusterId = aToCMap.get(article.getName());
                    int prevLength = 0;
                    int length;
                    while(true) {
                        context.progress();
                        Revision rev = parser.getNextRevision();
                        if (rev == null) {
                            break;
                        }
                        System.err.println("timestamp is " + rev.getTimestamp());
                        length = rev.getText().length();
                        String userId = rev.getContributor().getId();
                        if (delta.containsKey(userId)) {
                            delta.put(userId, delta.get(userId) + length - prevLength);
                        } else {
                            delta.put(userId, length - prevLength);
                        }
                        prevLength = length;
                    }
                    StringBuilder sb = new StringBuilder();
                    for (String userId : delta.keySet()) {
                        sb.append(userId).append("#").append(delta.get(userId)).append("|");
                    }
                    context.write(new Text(article.getId()), new Text(sb.toString()));
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
    
    public static class ClusterReducer extends Reducer<IntWritable,Text,Text,Text> {
        @Override
        public void reduce(IntWritable key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
            HashMap<Integer,Integer> deltas = new HashMap<Integer,Integer>();
            for(Text value : values) {
                for (String userDeltaPair : value.toString().split("\\|")) {
                    String[] ud = userDeltaPair.split("#");
                    int u = Integer.parseInt(ud[0]);
                    if (!deltas.containsKey(u)) {
                        deltas.put(u, Integer.parseInt(ud[1]));
                    } else {
                        deltas.put(u, deltas.get(u) + Integer.parseInt(ud[1]));
                    }
                }
            }
            StringBuilder sb = new StringBuilder();
            for (int i : deltas.keySet()) {
                sb.append(i).append("#").append(deltas.get(i)).append("|");
            }
            context.write(new Text("" + key.get()), new Text(sb.toString()));
        }
    }
    
    @Override
    public int run(String[] args) throws Exception {
        
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

        job.setJarByClass(DeltaByteCounter.class);
        job.setInputFormatClass(KeyValueTextInputFormat.class);
        job.setOutputFormatClass(TextOutputFormat.class);
        job.setMapOutputKeyClass(Text.class);
        job.setMapOutputValueClass(Text.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(Text.class);

        //DistributedCache.addCacheFile(new Path(args[2]).toUri(), job.getConfiguration());
        
        job.setMapperClass(PageMapper.class);
        job.setReducerClass(Reducer.class); // identity reducer
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
        int res = ToolRunner.run(new DeltaByteCounter(), args);
        System.exit(res);
    }
}

