// https://searchcode.com/api/result/102279522/

package project2;

/*
 * A single reducer summarizes output from pass3.
 */

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.StringTokenizer;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;

public class Pass4 {

  private static final boolean DEBUG = false;

  public static class Map extends Mapper<LongWritable, Text, Text, Text> {
    private Text dummyKey = new Text("key");

    public void map(LongWritable key, Text value, Context context) throws IOException {
      String line = value.toString();
      try {
        context.write(dummyKey, value);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }

  /*
   * Reducer receives a list of <gID, posRoot> pairs with the same gID. This method parse position
   * and root construct a matrix for this column group, process union-find again. Union-find
   * information on left-boundary remain the same as in input.
   */
  public static class Reduce extends Reducer<Text, Text, Text, Text> {

    public void reduce(Text key, Iterable<Text> value, Context context) throws IOException,
        InterruptedException {

      // a hashmap stores <root, size> from pass3
      HashMap<Long, Long> roots = new HashMap<Long, Long>();
      // a hashmap stores <position, root>
      HashMap<Long, Long> leftEdge = new HashMap<Long, Long>();
      // a hashmap stores <position, root>
      HashMap<Long, Long> rightEdge = new HashMap<Long, Long>();
      // a hashmap stores left common column <position, root> from pass-2 --> pass-3
      HashMap<Long, Long> commonCols = new HashMap<Long, Long>();
      String line = null, buf = null;
      StringTokenizer tokenizer;
      long position = 0, root = 0, temp = 0, count = 0, gid = 0, index = 0, lines = 0, size = 0, totalVertices =
          0, totalEdges = 0, maxCCSize = 0, minCCSize = Integer.MAX_VALUE, components = 0, realRoot =
          0;
      double avgCCSize = 0, weightedAvgSize = 0, avgBurnCount = 0;

      // read input
      Iterator ite = value.iterator();
      while (ite.hasNext()) {
        // read GROUP, , gid
        line = ite.next().toString();
        tokenizer = new StringTokenizer(line);
        buf = tokenizer.nextToken();

        if (buf.trim().equals("GROUP") || buf.trim() == "GROUP") {
          buf = tokenizer.nextToken();
          gid = Long.parseLong(buf);
        }

        // read PART1
        if (buf.trim().equals("PART1") || buf.trim() == "PART1") {
          lines = Long.parseLong(tokenizer.nextToken());
          for (long i = 0; i < lines; i++) {
            line = ite.next().toString();
            tokenizer = new StringTokenizer(line);
            root = Long.parseLong(tokenizer.nextToken());
            count = Long.parseLong(tokenizer.nextToken());
            roots.put(root, count);
          }
        }

        // read PART2
        if (buf.trim().equals("PART2") || buf.trim() == "PART2") {
          lines = Long.parseLong(tokenizer.nextToken());
          for (long i = 0; i < lines; i++) {
            line = ite.next().toString();
            tokenizer = new StringTokenizer(line);
            position = Long.parseLong(tokenizer.nextToken());
            root = Long.parseLong(tokenizer.nextToken());
            leftEdge.put(position, root);
          }
        }

        // read PART3
        if (buf.trim().equals("PART3") || buf.trim() == "PART3") {
          lines = Long.parseLong(tokenizer.nextToken());
          for (long i = 0; i < lines; i++) {
            line = ite.next().toString();
            tokenizer = new StringTokenizer(line);
            position = Long.parseLong(tokenizer.nextToken());
            root = Long.parseLong(tokenizer.nextToken());
            rightEdge.put(position, root);
          }
        }

        // read PART4
        if (buf.trim().equals("PART4") || buf.trim() == "PART4") {
          lines = Long.parseLong(tokenizer.nextToken());
          for (long i = 0; i < lines; i++) {
            line = ite.next().toString();
            tokenizer = new StringTokenizer(line);
            position = Long.parseLong(tokenizer.nextToken());
            root = Long.parseLong((tokenizer.nextToken()));
            commonCols.put(position, root);
          }
        }

        if (buf.trim().equals("PART5") || buf.trim() == "PART5") {}

        if (buf.trim().equals("VERTICES") || buf.trim() == "VERTICES")
          totalVertices += Long.parseLong(tokenizer.nextToken());

        if (buf.trim().equals("EDGES") || buf.trim() == "EDGES")
          totalEdges += Long.parseLong(tokenizer.nextToken());
      }

      // output
      if (DEBUG) {
        size = roots.keySet().size();
        context.write(new Text("PART1"), new Text(String.valueOf(size)));
        ite = roots.keySet().iterator();
        while (ite.hasNext()) {
          root = Long.parseLong(ite.next().toString());
          count = roots.get(root);
          context.write(new Text(String.valueOf(root)), new Text(String.valueOf(count)));
        }

        size = leftEdge.keySet().size();
        context.write(new Text("PART2"), new Text(String.valueOf(size)));
        ite = leftEdge.keySet().iterator();
        while (ite.hasNext()) {
          position = Long.parseLong(ite.next().toString());
          root = leftEdge.get(position);
          context.write(new Text(String.valueOf(position)), new Text(String.valueOf(root)));
        }

        size = rightEdge.keySet().size();
        context.write(new Text("PART3"), new Text(String.valueOf(size)));
        ite = rightEdge.keySet().iterator();
        while (ite.hasNext()) {
          position = Long.parseLong(ite.next().toString());
          root = rightEdge.get(position);
          context.write(new Text(String.valueOf(position)), new Text(String.valueOf(root)));
        }

        size = commonCols.keySet().size();
        context.write(new Text("PART4"), new Text(String.valueOf(size)));
        ite = commonCols.keySet().iterator();
        while (ite.hasNext()) {
          position = Long.parseLong(ite.next().toString());
          root = commonCols.get(position);
          context.write(new Text(String.valueOf(position)), new Text(String.valueOf(root)));
        }
      }


      // Set rootKeySet = roots.keySet();
      ArrayList<Long> vOnEdge = new ArrayList<Long>();
      HashSet<Long> rootKeySet = new HashSet<Long>();
      Iterator iteP = null;

      // copy roots.keyset
      iteP = roots.keySet().iterator();
      while (iteP.hasNext()) {
        rootKeySet.add(Long.parseLong(iteP.next().toString()));
      }
      iteP = null;

      if (DEBUG)
        context.write(new Text("rootKeySet size:"), new Text(String.valueOf(rootKeySet.size())));

      // combine and reduce
      long nLocalRoots = roots.keySet().size();
      for (long i = 0; i < nLocalRoots; i++) {

        if (DEBUG) {
          context.write(new Text("roots size:"), new Text(String.valueOf(roots.keySet().size())));
          context.write(new Text("i="), new Text(String.valueOf(i)));
        }

        ite = rootKeySet.iterator();
        // find the biggest root in rootKeySet
        long curRoot = Long.parseLong(ite.next().toString());
        while (ite.hasNext()) {
          temp = Long.parseLong(ite.next().toString());
          if (temp > curRoot) curRoot = temp;
        }
        realRoot = curRoot;
        if (DEBUG) context.write(new Text("RealRoot"), new Text(String.valueOf(realRoot)));

        // scan leftEdge and rightEdge, get list vOnEdge from <pos, root> having root = curRoot
        iteP = leftEdge.keySet().iterator();
        while (iteP.hasNext()) {
          temp = Long.parseLong(iteP.next().toString());
          if (leftEdge.get(temp) == curRoot) vOnEdge.add(temp);
        }
        if (DEBUG)
          context.write(new Text("points on left edge"), new Text(String.valueOf(vOnEdge.size())));

        iteP = rightEdge.keySet().iterator();
        while (iteP.hasNext()) {
          temp = Long.parseLong(iteP.next().toString());
          if (rightEdge.get(temp) == curRoot) {
            // temp must locate in the right side of curRoot
            long tempCol = temp / Pass1.COLUMN_GROUP_HEIGHT;
            long curRootCol = curRoot / Pass1.COLUMN_GROUP_HEIGHT;
            if (tempCol > curRootCol) vOnEdge.add(temp);
          }
        }
        if (DEBUG)
          context.write(new Text("points on both edge"), new Text(String.valueOf(vOnEdge.size())));

        // using all positions in vOnEdge, find smallest root: realRoot in commonCol
        iteP = commonCols.keySet().iterator();
        while (iteP.hasNext()) {
          temp = Long.parseLong(iteP.next().toString());
          if (vOnEdge.contains(temp)) {
            if (commonCols.get(temp) < realRoot) {
              realRoot = commonCols.get(temp);
            }
          }
        }
        if (DEBUG) context.write(new Text("real root is : "), new Text(String.valueOf(realRoot)));

        // update commonCols, for all positions in vOnEdge
        for (int j = 0; j < vOnEdge.size(); j++) {
          temp = vOnEdge.get(j); // get key
          commonCols.put(temp, realRoot);
        }

        if (realRoot == curRoot) {
          size = roots.get(curRoot);
        } else {
          size = roots.get(curRoot) + roots.get(realRoot);
        }

        if (realRoot != curRoot) {
          roots.remove(curRoot);
          roots.put(realRoot, size);
          if (DEBUG) context.write(new Text("Removed: "), new Text(String.valueOf(curRoot)));
        }

        // delete this root in rootKeySet
        rootKeySet.remove(curRoot);
        vOnEdge.clear();

        if (DEBUG) {
          context.write(new Text("Remove key"), new Text(String.valueOf(curRoot)));
          context.write(new Text("end"), new Text("end"));
          context.write(new Text(" "), new Text(" "));
        }
      }

      // calculation
      components = roots.size();
      ite = roots.keySet().iterator();
      while (ite.hasNext()) {
        root = Long.parseLong(ite.next().toString());
        size = roots.get(root);
        avgCCSize += size;
        avgBurnCount += size * size;
        weightedAvgSize += size * size;

        if (size > maxCCSize) maxCCSize = size;
        if (size < minCCSize) minCCSize = size;

        if (DEBUG) context.write(new Text(String.valueOf(root)), new Text(String.valueOf(size)));
      }

      if (components != 0) avgCCSize = avgCCSize / components;
      if (components == 0) minCCSize = maxCCSize;
      avgBurnCount = avgBurnCount / (Pass1.COLUMN_GROUP_HEIGHT * Pass1.COLUMN_GROUP_HEIGHT);
      weightedAvgSize = weightedAvgSize / totalVertices;

      // Print statistics
      context.write(new Text("Edges = "), new Text(String.valueOf(totalEdges)));
      context.write(new Text("Vertices = "), new Text(String.valueOf(totalVertices)));
      context.write(new Text("Components = "), new Text(String.valueOf(components)));
      context.write(new Text("Max CC size = "), new Text(String.valueOf(maxCCSize)));
      context.write(new Text("Min CC size = "), new Text(String.valueOf(minCCSize)));
      context.write(new Text("Avg CC size = "), new Text(String.valueOf(avgCCSize)));
      context.write(new Text("Weighted avg CC size = "), new Text(String.valueOf(weightedAvgSize)));
      context.write(new Text("Avg burn count = "), new Text(String.valueOf(avgBurnCount)));

    }
  }

  public static void main(String[] args) throws Exception {

    Configuration conf = new Configuration();

    Job job = new Job(conf, "CCPass4");
    job.setJarByClass(Pass4.class);


    job.setOutputKeyClass(Text.class);
    job.setOutputValueClass(Text.class);

    job.setMapperClass(project2.Pass4.Map.class);
    job.setReducerClass(project2.Pass4.Reduce.class);

    job.setInputFormatClass(TextInputFormat.class);
    job.setOutputFormatClass(TextOutputFormat.class);

    FileInputFormat.addInputPath(job, new Path(args[0]));
    // Set output path
    FileOutputFormat.setOutputPath(job, new Path(args[1]));

    job.waitForCompletion(true);

  }
}

