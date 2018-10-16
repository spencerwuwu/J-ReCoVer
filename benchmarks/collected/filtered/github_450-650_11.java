// https://searchcode.com/api/result/114133755/

package project2;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.StringTokenizer;
import java.util.TreeSet;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

public class Project2 {
  public static final double wmin = 0.092;
  public static final double wmax = 0.992;

  static enum ProjectCounters {
    VERTICES, COMPONENTS, EDGES, SQUAREDSUM
  };

  public static int vertices = 0;
  public static int edges = 0;
  public static int components = 0;
  public static double averageSize = 0;

  /*
   * Put each vertex that passes the filter in a group Mark with l or r if
   * vertex is on a boundary
   */
  public static class Pass1Mapper extends
      Mapper<Object, Text, IntWritable, Text> {
    IntWritable column = new IntWritable(0);
    IntWritable overlap = new IntWritable(0);
    Text word = new Text();
    double x;
    double y;
    double w;
    StringTokenizer itr;
    String currentLine;
    String[] currentLinePieces;
    int columnNum;

    public void map(Object key, Text value, Context context)
        throws IOException, InterruptedException {
      double lmax = context.getConfiguration().getFloat("lmax", 0);
      double g = context.getConfiguration().getFloat("g", 0);

      itr = new StringTokenizer(value.toString(), "\\n");

      while (itr.hasMoreTokens()) {
        currentLine = itr.nextToken();
        currentLinePieces = currentLine.split("\\s");
        x = new Double(currentLinePieces[0]);
        y = new Double(currentLinePieces[1]);
        w = new Double(currentLinePieces[2]);
        if ((w <= wmax) && (w >= wmin)) {
          columnNum = (int) Math.floor(x / g);
          column.set(columnNum);

          word.set(x + ":" + y);
          // left overlap
          if (x <= (g * columnNum) + (lmax / 2)) {
            if ((columnNum - 1) >= 0) {
              overlap.set(columnNum - 1);
              word.set(x + ":" + y + ":l");
              context.write(overlap, word);
              word.set(x + ":" + y + ":r");
            }
            context.write(column, word);
          }
          // right overlap
          else if (x >= (g * (columnNum + 1)) - (lmax / 2)) {
            if ((columnNum + 1) <= Math.ceil(1.0 / g)) {
              overlap.set(columnNum + 1);
              word.set(x + ":" + y + ":r");
              context.write(overlap, word);
              word.set(x + ":" + y + ":l");
            }
            context.write(column, word);
          }
          // no overlap
          else {
            context.write(column, word);
          }
          context.getCounter(ProjectCounters.VERTICES).increment(1);
        }
      }
    }
  }

  /*
   * Run a spatial join to find all connected components within a group
   */
  public static class Pass1Reducer extends
      Reducer<IntWritable, Text, IntWritable, Text> {
    Text pair = new Text();
    ArrayList<Point> points;
    ArrayList<Point> boundary;
    ArrayList<Edge> edges;
    String[] pieces;
    int j;
    TreeSet<Point> set;
    int i;
    double dist;
    SortedSet<Point> subset;

    public void reduce(IntWritable key, Iterable<Text> values, Context context)
        throws IOException, InterruptedException {
      double lmax = context.getConfiguration().getFloat("lmax", 0);
      points = new ArrayList<Point>();
      boundary = new ArrayList<Point>();
      edges = new ArrayList<Edge>();
      for (Text val : values) {
        pieces = val.toString().split(":");
        Point p = new Point(new Double(pieces[0]), new Double(pieces[1]));
        if (pieces.length == 3) {
          boundary.add(p);
        }
        points.add(p);
      }

      // spatial join
      Collections.sort(points);

      j = 0;
      set = new TreeSet<Point>(new CompareY());
      for (i = 0; i < points.size(); i++) {
        Point p1 = points.get(i);
        set.add(p1);
        while (points.get(j).getX() < (points.get(i).getX() - lmax)) {
          set.remove(points.get(j));
          j++;
        }

        Point finish = set.ceiling(new Point(p1.getX(), p1.getY() + lmax));
        Point start = set.ceiling(new Point(p1.getX(), p1.getY() - lmax));

        if (finish == null) {
          subset = set.tailSet(start);
        } else {
          subset = set.subSet(start, finish);
        }
        for (Point p : subset) {
          dist = (p1.x - p.x) * (p1.x - p.x) + (p1.y - p.y) * (p1.y - p.y);
          if (dist > 0 && dist <= (lmax * lmax)) {
            edges.add(new Edge(p, p1));
          }
        }
      }

      ConnectedComponent components = new ConnectedComponent(points);

      for (Edge e : edges) {
        components.union(e.p1, e.p2);
      }

      for (Point p : boundary) {
        pair.set(p.toString() + ";" + components.find(p).point.toString());
        context.write(key, pair);
      }
    }
  }

  /*
   * Set keys to 0 and pass through output
   */
  public static class Pass2Mapper extends
      Mapper<Object, Text, IntWritable, Text> {
    public void map(Object key, Text value, Context context)
        throws IOException, InterruptedException {
      IntWritable reduceKey = new IntWritable(0);
      Text word = new Text();
      StringTokenizer itr = new StringTokenizer(value.toString(), "\\n");
      String currentLine;
      while (itr.hasMoreTokens()) {
        currentLine = itr.nextToken();
        String[] pieces = currentLine.split("\\s");
        word.set(pieces[1]);
        context.write(reduceKey, word);
      }
    }
  }

  /*
   * Take explicit edges and perform union-find to find the root component for
   * each boundary point
   */
  public static class Pass2Reducer extends
      Reducer<IntWritable, Text, IntWritable, Text> {
    public void reduce(IntWritable key, Iterable<Text> values, Context context)
        throws IOException, InterruptedException {
      Text pair = new Text();
      TreeSet<Point> points = new TreeSet<Point>();
      ArrayList<Edge> edges = new ArrayList<Edge>();
      for (Text val : values) {
        String[] pieces = val.toString().split(";");

        String[] xy = pieces[0].split(":");
        Point p1 = new Point(new Double(xy[0]), new Double(xy[1]));
        xy = pieces[1].split(":");
        Point p2 = new Point(new Double(xy[0]), new Double(xy[1]));
        if (points.contains(p1)) {
          p1 = points.ceiling(p1);
        }
        if (points.contains(p2)) {
          p2 = points.ceiling(p2);
        }

        if (p1.equals(p2)) {
          points.add(p1);
        } else {
          edges.add(new Edge(p1, p2));

          points.add(p1);
          points.add(p2);
        }
      }

      ConnectedComponent components = new ConnectedComponent(points);
      for (Edge e : edges) {
        components.union(e.p1, e.p2);
      }
      for (Point p : points) {
        pair.set(p.toString() + ";" + components.find(p).point.toString());
        context.write(key, pair);
      }
    }
  }

  /*
   * Take new explicit edges, recompute implicit edges, and perform union-find
   * to find all the connected components
   */
  public static class Pass3Reducer extends
      Reducer<IntWritable, Text, IntWritable, Text> {
    static ArrayList<Edge> explicitEdges = new ArrayList<Edge>();
    static TreeSet<Point> edgePoints = new TreeSet<Point>();
    // static ArrayList<Point> edgePoints = new ArrayList<Point>();

    Text pair = new Text();
    TreeSet<Point> points;
    // ArrayList<Point> points;
    ArrayList<Edge> edges;
    String[] pieces;
    TreeSet<Point> set;
    int i;
    double dist;
    SortedSet<Point> subset;

    // reads the output of pass two and adds the explicit edges and points
    // involved
    public void setup(Context context) throws IOException {
      try {
        String outputPath = context.getConfiguration().get("outputPath");
        FileSystem fs = FileSystem.get(new URI(outputPath),
            context.getConfiguration());
        InputStreamReader in = new InputStreamReader(fs.open(new Path(
            outputPath + "/CC2/part-r-00000")));
        BufferedReader bin = new BufferedReader(in);
        String currentLine;
        while ((currentLine = bin.readLine()) != null) {
          String[] pieces = currentLine.split("\\s");
          pieces = pieces[1].split(";");
          Point p1 = new Point(pieces[0]);
          Point p2 = new Point(pieces[1]);
          edgePoints.add(p1);
          edgePoints.add(p2);
          explicitEdges.add(new Edge(p1, p2));
        }
      } catch (URISyntaxException e) {
        e.printStackTrace();
      }
    }

    @SuppressWarnings("unchecked")
    public void reduce(IntWritable key, Iterable<Text> values, Context context)
        throws IOException, InterruptedException {

      double lmax = context.getConfiguration().getFloat("lmax", 0);
      // Really make sure we setup
      if (edgePoints.size() == 0 || explicitEdges.size() == 0) {
        setup(context);
      }

      // add all the input points
      points = (TreeSet<Point>) edgePoints.clone();
      edges = (ArrayList<Edge>) explicitEdges.clone();
      for (Text val : values) {
        pieces = val.toString().split(":");
        Point p = new Point(new Double(pieces[0]), new Double(pieces[1]));
        points.add(p);

      }

      // spatial join on the points to get the implicit edges using sweep bound
      Iterator<Point> iter = points.iterator();
      Point j = iter.next();
      set = new TreeSet<Point>(new CompareY());
      for (Point p1 : points) {
        set.add(p1);
        while (j.getX() < (p1.getX() - lmax)) {
          set.remove(j);
          j = iter.next();
        }

        Point finish = set.ceiling(new Point(p1.getX(), p1.getY() + lmax));
        Point start = set.ceiling(new Point(p1.getX(), p1.getY() - lmax));

        if (finish == null) {
          subset = set.tailSet(start);
        } else {
          subset = set.subSet(start, finish);
        }
        for (Point p : subset) {
          dist = (p1.x - p.x) * (p1.x - p.x) + (p1.y - p.y) * (p1.y - p.y);
          if (dist > 0 && dist <= (lmax * lmax)) {
            edges.add(new Edge(p, p1));
          }
        }
      }

      // perform union-find with explicit and implicit edges to get all the connected components
      ConnectedComponent components = new ConnectedComponent(points);

      for (Edge e : edges) {
        components.union(e.p1, e.p2);
      }

      // write out (v, find(v))
      for (Point p : points) {
        pair.set(p.toString() + ";" + components.find(p).point.toString());
        context.write(key, pair);
      }
    }
  }

  /*
   * Turn each value into a key of root and value of point to root
   */
  public static class Pass4Mapper extends Mapper<Object, Text, Text, Text> {
    public void map(Object key, Text value, Context context)
        throws IOException, InterruptedException {
      Text word = new Text();
      Text zero = new Text();
      StringTokenizer itr = new StringTokenizer(value.toString(), "\\n");
      String currentLine;
      String[] pieces;
      Point p;
      Point root;
      while (itr.hasMoreTokens()) {
        currentLine = itr.nextToken();
        pieces = currentLine.split("[\\s;]");
        root = new Point(pieces[2]);
        p = new Point(pieces[1]);
        word.set(root + ";" + p);
        context.write(word, zero);
      }
    }
  }

  /*
   * Remove duplicates from groups
   */
  public static class Pass4Reducer extends Reducer<Text, Text, Text, Text> {
    public void reduce(Text key, Iterable<Text> values, Context context)
        throws IOException, InterruptedException {
      String[] pieces = key.toString().split(";");
      context.write(new Text(new Point(pieces[0]).toString()), new Text(
          new Point(pieces[1]).toString()));
      context.getCounter(ProjectCounters.VERTICES).increment(1);
    }
  }

  /*
   * Pass through keys and values
   */
  public static class Pass5Mapper extends
      Mapper<Object, Text, Text, IntWritable> {
    final static IntWritable one = new IntWritable(1);

    public void map(Object key, Text value, Context context)
        throws IOException, InterruptedException {
      Text root = new Text();
      StringTokenizer itr = new StringTokenizer(value.toString(), "\\n");
      String currentLine;
      String[] pieces;
      while (itr.hasMoreTokens()) {
        currentLine = itr.nextToken();
        pieces = currentLine.split("\\s");
        root.set(pieces[0]);
        context.write(root, one);
      }
    }
  }

  /*
   * Count components
   */
  public static class Pass5Reducer extends
      Reducer<Text, IntWritable, Text, IntWritable> {
    public void reduce(Text key, Iterable<IntWritable> values, Context context)
        throws IOException, InterruptedException {
      IntWritable result = new IntWritable();
      int sum = 0;
      for (IntWritable val : values) {
        sum += val.get();
      }
      result.set(sum);
      context.write(key, result);
      context.getCounter(ProjectCounters.COMPONENTS).increment(1);
      context.getCounter(ProjectCounters.SQUAREDSUM).increment(sum * sum);
    }
  }

  public static void main(String[] args) throws Exception {
    if (args.length != 4) {
      System.err
          .println("Usage (no trailing slashes): project2.Project2 lmax gfactor s3n://<in filename> s3n://<out bucket>");
      System.exit(2);
    }
    Configuration conf = new Configuration();
    double lmax = new Double(args[0]);
    double gfactor = new Double(args[1]);
    double g = lmax * gfactor;
    String inputPath = args[2];
    String outputPath = args[3];
    conf.setFloat("lmax", (float) lmax);
    conf.setFloat("g", (float) g);
    conf.set("inputPath", inputPath);
    conf.set("outputPath", outputPath);
    System.out.println("Reading input from: " + inputPath);
    System.out.println("Output to: " + outputPath);

    // Pass1 filters out unused vertices and finds implicit edges per group
    Job job1 = new Job(conf, "connected components 1");
    job1.setJarByClass(Project2.class);
    job1.setMapperClass(Pass1Mapper.class);
    job1.setReducerClass(Pass1Reducer.class);
    job1.setOutputKeyClass(IntWritable.class);
    job1.setOutputValueClass(Text.class);
    FileInputFormat.addInputPath(job1, new Path(inputPath));
    FileOutputFormat.setOutputPath(job1, new Path(outputPath + "/CC1"));
    job1.waitForCompletion(true);
    // Pass2 finds explicit edges between groups
    Job job2 = new Job(conf, "connected components 2");
    job2.setJarByClass(Project2.class);
    job2.setMapperClass(Pass2Mapper.class);
    job2.setReducerClass(Pass2Reducer.class);
    job2.setOutputKeyClass(IntWritable.class);
    job2.setOutputValueClass(Text.class);
    FileInputFormat.addInputPath(job2, new Path(outputPath + "/CC1"));
    FileOutputFormat.setOutputPath(job2, new Path(outputPath + "/CC2"));
    job2.waitForCompletion(true);
    // Pass3 combines the implicit edges per group and explicit edges
    Job job3 = new Job(conf, "connected components 3");
    job3.setJarByClass(Project2.class);
    job3.setMapperClass(Pass1Mapper.class); // Use Pass1 Mapper
    job3.setReducerClass(Pass3Reducer.class);
    job3.setOutputKeyClass(IntWritable.class);
    job3.setOutputValueClass(Text.class);
    FileInputFormat.addInputPath(job3, new Path(inputPath));
    FileOutputFormat.setOutputPath(job3, new Path(outputPath + "/CC3"));
    job3.waitForCompletion(true);
    // Pass 4 removes duplicate edges from the graph and finds the number of
    // vertices and edges
    Job job4 = new Job(conf, "connected components 4");
    job4.setJarByClass(Project2.class);
    job4.setMapperClass(Pass4Mapper.class);
    job4.setReducerClass(Pass4Reducer.class);
    job4.setOutputKeyClass(Text.class);
    job4.setOutputValueClass(Text.class);
    FileInputFormat.addInputPath(job4, new Path(outputPath + "/CC3"));
    FileOutputFormat.setOutputPath(job4, new Path(outputPath + "/CC4"));
    job4.waitForCompletion(true);
    // Pass 5 removes duplicate edges from the graph and finds the number of
    // vertices and edges
    Job job5 = new Job(conf, "connected components 5");
    job5.setJarByClass(Project2.class);
    job5.setMapperClass(Pass5Mapper.class);
    job5.setReducerClass(Pass5Reducer.class);
    job5.setOutputKeyClass(Text.class);
    job5.setOutputValueClass(IntWritable.class);
    FileInputFormat.addInputPath(job5, new Path(outputPath + "/CC4"));
    FileOutputFormat.setOutputPath(job5, new Path(outputPath + "/CC5"));
    job5.waitForCompletion(true);

    vertices += (int) job4.getCounters().findCounter(ProjectCounters.VERTICES)
        .getValue();
    components += (int) job5.getCounters()
        .findCounter(ProjectCounters.COMPONENTS).getValue();
    edges = EdgeFinder.run(conf, inputPath, outputPath);
    averageSize = (1.0 * (int) job5.getCounters()
        .findCounter(ProjectCounters.SQUAREDSUM).getValue())
        / vertices;

    String output = "===Input===\n" + inputPath + " Input Path\n" + outputPath
        + " Output Path\n" + lmax + " Lmax\n" + gfactor
        + " gfactor (Lmax multiple)\n" + g + " g (size of group)\n"
        + "===Results===\n" + vertices + " Vertices\n" + edges + " Edges\n"
        + components + " Connected Components\n" + averageSize
        + " Weighted Average Size of Components";
    System.out.println(output);

    FileSystem fs = FileSystem.get(new URI(outputPath), conf);
    OutputStreamWriter os = new OutputStreamWriter(fs.create(new Path(
        outputPath + "/output.txt")));
    BufferedWriter bwr = new BufferedWriter(os);
    bwr.write(output, 0, output.length());
    bwr.close();
  }
}
