// https://searchcode.com/api/result/102279542/

package project2;

/*
 * Connected Components Calculation Pass 1. In this pass, input data will be split into multiple
 * chunks. Each chunk is of length m*(g+1), standing for a "Column Group". The mapper reads
 * fixed-length input, identifies those points of "1" as vertex, and generates output of pair <G,
 * p>, where p is a position of a vertex in G. The reducer receives tuples and using UNION-FIND
 * algorithm to calculate localized connected components information for G.
 */
import java.io.IOException;
import java.util.HashMap;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;

public class Pass1 {

  public static int COLUMN_GROUP_WIDTH = 125;
  public static int COLUMN_GROUP_HEIGHT = 20000;
  private static final float fromNetID = (float) 0.459;
  private static final float desiredDensity = (float) 0.59;
  private static final float wMin = (float) (0.4 * fromNetID);
  private static final float wLimit = wMin + desiredDensity;

  public static class Map extends Mapper<LongWritable, Text, Text, IntWritable> {
    private Text gID = new Text();
    private int position = 0;
    private int posLocal = 0;

    public void map(LongWritable key, Text value, Context context) throws IOException {

      if (position < 400000000) {

        String line = value.toString();
        int row = Common.getRow(position);
        int col = Common.getCol(position);
        int gid = col / COLUMN_GROUP_WIDTH;
        float w = Float.parseFloat(line);
        Boolean tree = (w >= wMin && w < wLimit) ? true : false;

        if (tree) {
          try {
            // if this column is a boundary column
            if (col % COLUMN_GROUP_WIDTH == 0) {
              // send to left column group
              if (gid != 0) {
                gID.set(String.valueOf(gid - 1));
                posLocal = COLUMN_GROUP_WIDTH * COLUMN_GROUP_HEIGHT + row;
                context.write(gID, new IntWritable(posLocal));
              }
              // send to current column group
              gID.set(String.valueOf(gid));
              posLocal = row;
              context.write(gID, new IntWritable(posLocal));
            } else {
              gID.set(String.valueOf(gid));
              posLocal = (col % COLUMN_GROUP_WIDTH) * COLUMN_GROUP_HEIGHT + row;
              context.write(gID, new IntWritable(posLocal));
            }
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
        }
      }
      position++;
    }
  }

  public static class Reduce extends Reducer<Text, IntWritable, Text, Text> {

    public void reduce(Text gID, Iterable<IntWritable> positions, Context context)
        throws IOException, InterruptedException {

      // Initialize matrix, default all false
      HashMap<Integer, Boolean> matrix = new HashMap<Integer, Boolean>();
      // initialize unionfind
      UnionFind u = new UnionFind((COLUMN_GROUP_WIDTH + 1) * COLUMN_GROUP_HEIGHT);

      int gid = Integer.parseInt(gID.toString());
      // update matrix from Iterable<IntWritable> positions
      int position = 0, posLocal = 0;

      for (IntWritable p : positions) {
        position = p.get();
        int col = position / COLUMN_GROUP_HEIGHT;
        int row = position % COLUMN_GROUP_HEIGHT;
        posLocal = col * Pass1.COLUMN_GROUP_HEIGHT + row;
        matrix.put(posLocal, true);
      }

      // traverse the matrix, process union-find algorithm
      int nRow = Pass1.COLUMN_GROUP_HEIGHT; // height
      int nCol = Pass1.COLUMN_GROUP_WIDTH + 1; // width


      for (int col = 0; col < nCol; col++) {
        for (int row = 0; row < nRow; row++) {
          // if this entry has tree
          posLocal = col * Pass1.COLUMN_GROUP_HEIGHT + row;
          if (matrix.containsKey(posLocal)) {
            // if the vertex is connected with the one to its east
            if (col + 1 < nCol && matrix.containsKey(posLocal + Pass1.COLUMN_GROUP_HEIGHT)) {
              u.union(Common.getPos(col, row, nRow), Common.getPos(col + 1, row, nRow));
            }
            // if the vertex is connected with the one to its north
            if (row + 1 < nRow && matrix.containsKey(posLocal + 1)) {
              u.union(Common.getPos(col, row, nRow), Common.getPos(col, row + 1, nRow));
            }
          } else {
            // if this entry has no tree
            int index = Common.getPos(col, row, nRow);
            u.array[index] = 0;
          }
        }
      }

      int pos = 0, rootLocal = 0, absPos = 0, absRootLocal = 0;
      // traverse the matrix, for each 'vertex', find its root, and output as TupleWritable
      for (int col = 0; col < nCol; col++) {
        for (int row = 0; row < nRow; row++) {
          posLocal = col * COLUMN_GROUP_HEIGHT + row;
          if (matrix.containsKey(posLocal)) {
            pos = Common.getPos(col, row, nRow);
            // find the root of the CC
            rootLocal = u.find(pos);
            // change to absolute coordinates.
            absPos = COLUMN_GROUP_WIDTH * COLUMN_GROUP_HEIGHT * gid + pos;
            absRootLocal = COLUMN_GROUP_WIDTH * COLUMN_GROUP_HEIGHT * gid + rootLocal;

            String t = String.valueOf(absPos) + " " + String.valueOf(absRootLocal);

            try {
              context.write(gID, new Text(t));

            } catch (InterruptedException e) {
              e.printStackTrace();
            }
          }
        }
      }
    }
  }

  public static void main(String[] args) throws Exception {

    Configuration conf = new Configuration();

    Job job = new Job(conf, "CCPass1");
    job.setJarByClass(Pass1.class);

    job.setOutputKeyClass(Text.class);
    job.setOutputValueClass(IntWritable.class);

    job.setMapperClass(project2.Pass1.Map.class);
    job.setReducerClass(project2.Pass1.Reduce.class);

    job.setInputFormatClass(TextInputFormat.class);
    job.setOutputFormatClass(TextOutputFormat.class);

    FileInputFormat.addInputPath(job, new Path(args[0]));
    FileOutputFormat.setOutputPath(job, new Path(args[1]));


    job.waitForCompletion(true);

  }
}

