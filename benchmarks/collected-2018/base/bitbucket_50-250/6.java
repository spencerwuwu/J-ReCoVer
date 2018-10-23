// https://searchcode.com/api/result/102279530/

package project2;

/*
 * A mapreduce pass input <position, root> pair from both pass-2 and pass-3 result (possible
 * duplicate on common column vertices). This pass updates common column information in pass-2
 * result with pass-3's. Then split into column groups as they were in pass-1, and process local
 * union-find again. Store information on disk for next pass(es) to process calculation.
 */

import java.io.IOException;
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

public class Pass3Deprecated {

  public static class Map extends Mapper<LongWritable, Text, Text, Text> {
    private Text gID = new Text();
    private Text posRoot = new Text();

    public void map(LongWritable key, Text value, Context context) throws IOException {
      String line = value.toString();
      StringTokenizer tokenizer = new StringTokenizer(line);
      String gIDString, positionString, rootString;

      // if the tuple comes from pass-1, <G, p, q>
      if (tokenizer.countTokens() == 3) {
        gIDString = tokenizer.nextToken();
        positionString = tokenizer.nextToken();
        rootString = tokenizer.nextToken();

      } else {
        // if the tuple comes from pass-2, <p, q>
        positionString = tokenizer.nextToken();
        rootString = tokenizer.nextToken();
      }

      int position = Integer.parseInt(positionString);

      int col = position / Pass1.COLUMN_GROUP_HEIGHT;
      // key field to pass to reducer
      int gid = col / Pass1.COLUMN_GROUP_WIDTH;
      // value field to pass to reducer
      String s = positionString + " " + rootString;
      posRoot.set(s);

      try {
        // if position is at a boundary, send to left column group
        if (col % Pass1.COLUMN_GROUP_WIDTH == 0 && gid != 0) {
          gID.set(String.valueOf(gid - 1));
          context.write(gID, posRoot);
        }
        // send to current column group
        gID.set(String.valueOf(gid));
        context.write(gID, posRoot);

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

    public void reduce(Text gID, Iterable<Text> value, Context context) throws IOException,
        InterruptedException {

      String line = null, delim = "[ ]";
      String[] tokens;
      int position = 0, index = 0, col = 0, row = 0, root = 0;
      int gid = Integer.parseInt(gID.toString());

      // Initialize a local matrix, default all false
      boolean[][] matrix = new boolean[Pass1.COLUMN_GROUP_WIDTH + 1][Pass1.COLUMN_GROUP_HEIGHT];
      // initialize unionfind
      UnionFind u = new UnionFind((Pass1.COLUMN_GROUP_WIDTH + 1) * Pass1.COLUMN_GROUP_HEIGHT);

      // update matrix, and unionfind with input <position, root> pairs. Take smaller root value if
      // duplicate <position, root*> detected
      for (Text t : value) {
        line = t.toString();
        tokens = line.split(delim);
        // get absolute position
        position = Integer.parseInt(tokens[0]);
        root = Integer.parseInt(tokens[1]);

        // update matrix (change to local position first)
        index = position - gid * Pass1.COLUMN_GROUP_HEIGHT * Pass1.COLUMN_GROUP_WIDTH;
        col = index / Pass1.COLUMN_GROUP_HEIGHT;
        row = index % Pass1.COLUMN_GROUP_HEIGHT;
        matrix[col][row] = true;

        // update union-find array
        if (u.array[index] == -1) u.array[index] = root;
        if (u.array[index] > root) u.array[index] = root;
      }

      // traverse the matrix (omit column 0), process union-find algorithm
      int nRow = matrix[0].length;
      int nCol = matrix.length;
      int indexP = 0;
      index = 0;

      for (col = 0; col < nCol; col++) {
        for (row = 0; row < nRow; row++) {
          // if this entry has tree
          if (matrix[col][row]) {
            index = col * Pass1.COLUMN_GROUP_HEIGHT + row;
            // if the vertex is connected with the one to its east
            if (col + 1 < nCol && matrix[col + 1][row]) {
              indexP = index + Pass1.COLUMN_GROUP_HEIGHT;

              if (u.array[indexP] != u.array[index]) {
                if (u.array[indexP] > u.array[index]) u.array[indexP] = u.array[index];
                if (u.array[indexP] < u.array[index]) u.array[index] = u.array[indexP];
              }
            }

            // if the vertex is connected with the one to its north
            if (row + 1 < nRow && matrix[col][row + 1]) {
              indexP = index + 1;

              if (u.array[indexP] != u.array[index]) {
                if (u.array[indexP] > u.array[index]) u.array[indexP] = u.array[index];
                if (u.array[indexP] < u.array[index]) u.array[index] = u.array[indexP];
              }
            }

          }
        }
      }

      // output
      index = 0;
      root = 0;
      for (col = 0; col < nCol; col++) {
        for (row = 0; row < nRow; row++) {
          if (matrix[col][row]) {
            // index = local position
            index = Common.getPos(col, row, nRow);
            // get absolute position
            position = index + gid * Pass1.COLUMN_GROUP_HEIGHT * Pass1.COLUMN_GROUP_WIDTH;
            // get absolute root from union array
            root = u.array[index];

            String s = String.valueOf(position) + " " + String.valueOf(root);
            context.write(gID, new Text(String.valueOf(s)));
          }
        }
      }

    }
  }

  public static void main(String[] args) throws Exception {

    Configuration conf = new Configuration();

    Job job = new Job(conf, "CCPass3");
    job.setJarByClass(Pass3.class);


    job.setOutputKeyClass(Text.class);
    job.setOutputValueClass(Text.class);

    job.setMapperClass(project2.Pass3.Map.class);
    job.setReducerClass(project2.Pass3.Reduce.class);

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

