// https://searchcode.com/api/result/102279526/

package project2;

/*
 * A mapreduce pass input result from Pass1, extract all boundary columns, pass to reducer,
 * construct a matrix and process union-find on all boundary columns. Then resume their index and
 * output.
 */
import java.io.IOException;
import java.util.HashMap;
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

public class Pass2 {

  public static class Map extends Mapper<LongWritable, Text, Text, Text> {
    private Text dummyKey = new Text("key");

    public void map(LongWritable key, Text value, Context context) throws IOException {
      String line = value.toString();
      StringTokenizer tokenizer = new StringTokenizer(line);

      String gID = tokenizer.nextToken();
      String positionString = tokenizer.nextToken();
      String rootString = tokenizer.nextToken();
      int position = Integer.parseInt(positionString);

      // if p is at boundary, send <p,q> to a single reducer
      // include left-most column
      if (position % (Pass1.COLUMN_GROUP_HEIGHT * Pass1.COLUMN_GROUP_WIDTH) < Pass1.COLUMN_GROUP_HEIGHT) {

        try {
          String s = positionString + " " + rootString;
          context.write(dummyKey, new Text(s));
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
    }
  }

  /*
   * Reducer receives <p,q> pair with p on a boundary. There exists duplicate <key, value> pair with
   * a given key for p at common columns.
   */
  public static class Reduce extends Reducer<Text, Text, Text, Text> {

    // value in form <position, root>
    public void reduce(Text key, Iterable<Text> value, Context context) throws IOException,
        InterruptedException {

      // Initialize a local matrix to store all boundary columns
      // width = # column groups
      int width = Pass1.COLUMN_GROUP_HEIGHT / Pass1.COLUMN_GROUP_WIDTH;
      // boolean[][] matrix = new boolean[width][Pass1.COLUMN_GROUP_HEIGHT];
      HashMap<Integer, Boolean> matrix = new HashMap<Integer, Boolean>();
      UnionFind u = new UnionFind(width * Pass1.COLUMN_GROUP_HEIGHT);

      String positionString = null, rootString = null, line = null, delim = "[ ]";
      String[] tokens;
      int col = 0, row = 0, tmp = 0, position = 0, index = 0, root = 0, pR = 0;

      // update matrix and union-find array
      for (Text t : value) {
        line = t.toString();
        tokens = line.split(delim);
        positionString = tokens[0];
        rootString = tokens[1];
        position = Integer.parseInt(positionString);
        root = Integer.parseInt(rootString);

        col = position / (Pass1.COLUMN_GROUP_HEIGHT * Pass1.COLUMN_GROUP_WIDTH);
        row = position % Pass1.COLUMN_GROUP_HEIGHT;
        // matrix[col][row] = true;

        index = col * Pass1.COLUMN_GROUP_HEIGHT + row;
        matrix.put(index, true);

        // update union-find array
        if (u.array[index] == -1) u.array[index] = root;
        if (u.array[index] > root) u.array[index] = root;
      }


      /*
       * Special union-find for each vertex A in this matrix, get root(A), if root(A) drops in this
       * matrix (index re-mapped), let C = root(A) if root(C) <= root(A), => root(A) = root(C)
       */
      int nRow = Pass1.COLUMN_GROUP_HEIGHT;
      int nCol = Pass1.COLUMN_GROUP_WIDTH + 1;
      int colRoot = 0, rowRoot = 0, indexRoot = 0;

      for (col = 1; col < nCol; col++) {
        for (row = 0; row < nRow; row++) {
          index = col * Pass1.COLUMN_GROUP_HEIGHT + row;
          if (matrix.containsKey(index)) {
            root = u.array[index];
            // if root drops at boundaries (in this matrix)
            if (root % (Pass1.COLUMN_GROUP_HEIGHT * Pass1.COLUMN_GROUP_WIDTH) < Pass1.COLUMN_GROUP_HEIGHT) {
              // get the root entry
              colRoot = root / (Pass1.COLUMN_GROUP_HEIGHT * Pass1.COLUMN_GROUP_WIDTH);
              rowRoot = root % Pass1.COLUMN_GROUP_HEIGHT;

              indexRoot = colRoot * Pass1.COLUMN_GROUP_HEIGHT + rowRoot;
              if (matrix.containsKey(indexRoot)) {
                // Root points to a lower vertex, update array[index]
                if (u.array[indexRoot] < u.array[index]) {
                  u.array[index] = u.array[indexRoot];
                }
              }
            }
          }
        }
      }

      // output updated <position, root> at boundaries
      for (col = 0; col < nCol; col++) {
        for (row = 0; row < nRow; row++) {
          index = col * Pass1.COLUMN_GROUP_HEIGHT + row;
          if (matrix.containsKey(index)) {
            // absolute position
            position = col * (Pass1.COLUMN_GROUP_HEIGHT * Pass1.COLUMN_GROUP_WIDTH) + row;
            // absolute root
            root = u.array[index];

            context.write(new Text(String.valueOf(position)), new Text(String.valueOf(root)));
          }
        }
      }
    }
  }

  public static void main(String[] args) throws Exception {

    Configuration conf = new Configuration();

    Job job = new Job(conf, "CCPass2");
    job.setJarByClass(Pass2.class);


    job.setOutputKeyClass(Text.class);
    job.setOutputValueClass(Text.class);

    job.setMapperClass(project2.Pass2.Map.class);
    job.setReducerClass(project2.Pass2.Reduce.class);

    job.setInputFormatClass(TextInputFormat.class);
    job.setOutputFormatClass(TextOutputFormat.class);

    FileInputFormat.addInputPath(job, new Path(args[0]));
    FileOutputFormat.setOutputPath(job, new Path(args[1]));

    job.waitForCompletion(true);

  }
}

