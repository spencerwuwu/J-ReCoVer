// https://searchcode.com/api/result/102279538/

package project2;

/*
 * A mapreduce pass input <position, root> pair from both pass-2 and pass-3 result (possible
 * duplicate on common column vertices). This pass updates common column information in pass-2
 * result with pass-3's. Then split into column groups as they were in pass-1, and process local
 * union-find again. Store information on disk for next pass(es) to process calculation.
 */

import java.io.IOException;
import java.util.HashMap;
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

public class Pass3ExtraCredit {

  public static class Map extends Mapper<LongWritable, Text, Text, Text> {
    private Text gID = new Text();
    private Text posRoot = new Text();

    public void map(LongWritable key, Text value, Context context) throws IOException {
      String line = value.toString();
      StringTokenizer tokenizer = new StringTokenizer(line);
      String gIDString, positionString, rootString, s;
      long position = 0;

      try {
        // if the tuple comes from pass-1, send <G, p, q>
        if (tokenizer.countTokens() == 3) {
          gIDString = tokenizer.nextToken();
          gID.set(gIDString);
          context.write(gID, value);

        } else {
          // if the tuple comes from pass-2, <p, q>
          positionString = tokenizer.nextToken();
          rootString = tokenizer.nextToken();
          position = Long.parseLong(positionString);
          long col = position / Pass1.COLUMN_GROUP_HEIGHT;
          long gid = col / Pass1.COLUMN_GROUP_WIDTH;
          s = positionString + " " + rootString;
          gID.set(String.valueOf(gid));
          posRoot.set(s);
          context.write(gID, posRoot);
        }
      } catch (InterruptedException e) {
        e.printStackTrace();
      }

    }
  }

  /*
   * Reducer receives a list of <G, p, q> or <gID, posRoot> pairs with the same gID. This method
   * parse position and root construct a matrix for this column group, process union-find again.
   * Union-find information on left-boundary remain the same as in input.
   */
  public static class Reduce extends Reducer<Text, Text, Text, Text> {

    public void reduce(Text gID, Iterable<Text> value, Context context) throws IOException,
        InterruptedException {

      String line = null, delim = "[ ]", gIDString = null;
      StringTokenizer tokenizer;
      String[] tokens;
      long position = 0, root = 0, temp = 0, count = 0, north = 0, east = 0, northEast = 0, southEast =
          0, northWest = 0, index = 0, size = 0;
      long gid = Long.parseLong(gID.toString());

      long vertices = 0, edges = 0;

      // a hashmap stores <position, root> from pass-1, 11 columns (0~10)
      HashMap<Long, Long> pairs = new HashMap<Long, Long>();
      // a hashmap stores <position, root> from pass-2, col = 10
      HashMap<Long, Long> commonCols = new HashMap<Long, Long>();
      // a hashmap stores <root, size>
      HashMap<Long, Long> roots = new HashMap<Long, Long>();
      // a hashmap stores <position, root>
      HashMap<Long, Long> leftEdge = new HashMap<Long, Long>();
      // a hashmap stores <position, root>
      HashMap<Long, Long> rightEdge = new HashMap<Long, Long>();

      // if countTokens == 3, the tuple comes from pass-1, otherwise from pass-2
      for (Text t : value) {
        line = t.toString();
        tokenizer = new StringTokenizer(line);
        // if tuple comes from pass-1
        if (tokenizer.countTokens() == 3) {
          gIDString = tokenizer.nextToken();
          position = Long.parseLong(tokenizer.nextToken());
          root = Long.parseLong(tokenizer.nextToken());

          pairs.put(position, root);
          vertices++;

          // put the local left column, col = 0
          if (position - (gid * Pass1.COLUMN_GROUP_HEIGHT * Pass1.COLUMN_GROUP_WIDTH) < Pass1.COLUMN_GROUP_HEIGHT)
            leftEdge.put(position, root);
          // put the local right column, col = 10
          if (position - ((gid + 1) * Pass1.COLUMN_GROUP_HEIGHT * Pass1.COLUMN_GROUP_WIDTH) >= 0) {
            rightEdge.put(position, root);
            vertices--;
          }

        } else {
          // if tuple comes from pass-2
          position = Long.parseLong(tokenizer.nextToken());
          root = Long.parseLong(tokenizer.nextToken());
          commonCols.put(position, root);
        }
      }

      // go through hashmap pairs, convert to hashmap <root, size>
      Iterator<Long> ite = pairs.keySet().iterator();
      while (ite.hasNext()) {
        // get key: position
        position = Long.parseLong(ite.next().toString());

        // only count left 10 columns to root
        if (position < ((gid + 1) * Pass1.COLUMN_GROUP_HEIGHT * Pass1.COLUMN_GROUP_WIDTH)) {
          root = pairs.get(position);

          // the root must locate in left 10 columns, (0~9, not include col = 10)
          if (roots.containsKey(root)) {
            count = roots.get(root);
            count++;
            roots.put(root, count);
          } else {
            roots.put(root, (long) 1);
          }
        }

      }

      // calculate edges
      for (long col = 0; col < Pass1.COLUMN_GROUP_WIDTH + 1; col++) {
        for (long row = 0; row < Pass1.COLUMN_GROUP_HEIGHT; row++) {
          index = col * Pass1.COLUMN_GROUP_HEIGHT + row;
          position = gid * Pass1.COLUMN_GROUP_HEIGHT * Pass1.COLUMN_GROUP_WIDTH + index;
          north = position + 1;
          east = position + Pass1.COLUMN_GROUP_HEIGHT;
          northEast = position + Pass1.COLUMN_GROUP_HEIGHT + 1;
          southEast = position + Pass1.COLUMN_GROUP_HEIGHT - 1;
          northWest = position - Pass1.COLUMN_GROUP_HEIGHT + 1;

          if (pairs.containsKey(position)) {
            if (pairs.containsKey(east)) edges++;
            if (pairs.containsKey(northWest)) edges++;
            if (pairs.containsKey(northEast)) edges++;
            if (row + 1 < Pass1.COLUMN_GROUP_HEIGHT && pairs.containsKey(north)) {
              // if position is on right edge, don't count north edge
              if (col < Pass1.COLUMN_GROUP_WIDTH) edges++;
            }
          }
        }
      }

      pairs.clear();

      /*
       * Output, start with <GROUP, gid>; Followed by <PART*, #lines>, from PART1 - PART4; PART5 has
       * two lines for vertices and edges.
       */
      context.write(new Text("GROUP"), gID);

      size = roots.keySet().size();
      context.write(new Text("PART1"), new Text(String.valueOf(size)));
      ite = roots.keySet().iterator();
      while (ite.hasNext()) {
        root = Long.parseLong(ite.next().toString());
        count = roots.get(root);
        context.write(new Text(String.valueOf(root)), new Text(String.valueOf(count)));
      }
      roots.clear();

      size = leftEdge.keySet().size();
      context.write(new Text("PART2"), new Text(String.valueOf(size)));
      ite = leftEdge.keySet().iterator();
      while (ite.hasNext()) {
        position = Long.parseLong(ite.next().toString());
        root = leftEdge.get(position);
        context.write(new Text(String.valueOf(position)), new Text(String.valueOf(root)));
      }
      leftEdge.clear();

      size = rightEdge.keySet().size();
      context.write(new Text("PART3"), new Text(String.valueOf(size)));
      ite = rightEdge.keySet().iterator();
      while (ite.hasNext()) {
        position = Long.parseLong(ite.next().toString());
        root = rightEdge.get(position);
        context.write(new Text(String.valueOf(position)), new Text(String.valueOf(root)));
      }
      rightEdge.clear();

      size = commonCols.keySet().size();
      context.write(new Text("PART4"), new Text(String.valueOf(size)));
      ite = commonCols.keySet().iterator();
      while (ite.hasNext()) {
        position = Long.parseLong(ite.next().toString());
        root = commonCols.get(position);
        context.write(new Text(String.valueOf(position)), new Text(String.valueOf(root)));
      }
      commonCols.clear();

      context.write(new Text("PART5"), new Text("2"));
      context.write(new Text("VERTICES"), new Text(String.valueOf(vertices)));
      context.write(new Text("EDGES"), new Text(String.valueOf(edges)));
    }
  }

  public static void main(String[] args) throws Exception {

    Configuration conf = new Configuration();

    Job job = new Job(conf, "CCPass3ExtraCredit");
    job.setJarByClass(Pass3ExtraCredit.class);


    job.setOutputKeyClass(Text.class);
    job.setOutputValueClass(Text.class);

    job.setMapperClass(project2.Pass3ExtraCredit.Map.class);
    job.setReducerClass(project2.Pass3ExtraCredit.Reduce.class);

    job.setInputFormatClass(TextInputFormat.class);
    job.setOutputFormatClass(TextOutputFormat.class);

    // Input union of pass-1 and pass-2
    FileInputFormat.addInputPath(job, new Path(args[0]));
    FileInputFormat.addInputPath(job, new Path(args[1]));
    // Set output path
    FileOutputFormat.setOutputPath(job, new Path(args[2]));

    job.waitForCompletion(true);

  }
}

