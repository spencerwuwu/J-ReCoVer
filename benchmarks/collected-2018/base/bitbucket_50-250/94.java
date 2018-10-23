// https://searchcode.com/api/result/123156382/

package fogbow.dicta13.memenetwork.stringsim;

import java.util.*;
import java.io.*;
import java.text.*;
import org.apache.hadoop.io.*;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.Partitioner;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.apache.log4j.Logger;
import edu.umd.cloud9.io.array.ArrayListWritable;

public class ComputeSimilarity extends Configured implements Tool{
  private static final Logger sLogger = Logger.getLogger (ComputeSimilarity.class);
  private static class MyMapper extends Mapper < LongWritable, Text, Text, Text >{
    public void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {

			/*
			Map<String,Float> map=new LinkedHashMap<String,Float>();
			FileReader fr = new FileReader(dfscore.txt);
			BufferedReader reader = new BufferedReader(fr);
			String st = "",str=" ";
			while((st = reader.readLine()) != null) {
				String[] worddf = st.split("\\t+");
				map.put(worddf[0], Float.parseFloat(worddf[1]);
			}
			*/
			String[] pair = ((Text) value).toString().split("\\t+\\s*", 2);
			
			//article[0] contains article number. article[1] contains the text.
			//StringTokenizer stc = CleanStringTokenizer.apply(article[1]);	
			
			//BreakIterator bi = BreakIterator.getSentenceInstance();
			//bi.setText(article[1]);
			//int index = 0;
			
			//while (bi.next() != BreakIterator.DONE) {
				//sentence = article[1].substring(index, bi.current()).toLowerCase().replaceAll("[^a-z0-9\\s]+", " ");
				//replace with spaces instead of nulls, keeps hyphenated words separate at cost of spacing
				//String[] tokens = sentence.split("\\s+");
				//for (Integer i = 0; i < tokens.length - (N-1); i++){
				//StringBuilder ngram = new StringBuilder();
					//for (int j = 0; j < N; j++){
						//ngram.append(tokens[i+j] + " ");
					//}
					//context.write (new Text(ngram.toString().trim()), new Text(article[0].trim()));
					//context.write (new Text(ngram.toString().trim()), new Text(article[0].trim()+"~"+sentence));
					//If no unique senteceID, emit full sentence
				//}
				//index = bi.current();
			}
    }
  }
  

  private static class MyReducer extends Reducer < Text, Text, Text, Text >
  {
    //private static Text outputkey = new Text();
    private static Text outputval = new Text();
    @Override
    public void reduce (Text key, Iterable <Text> values, Context context) throws IOException, InterruptedException{
			int counter = 0;
	    StringBuilder sb = new StringBuilder();     //for formatting output
      for (Text currentValue : values) {
				counter++;
				sb.append(currentValue.toString());
				sb.append("\t");
				if (counter>=500){		//Split up lines with more than 500 pairs. Avoids memory erros in next phase.
					sb.append("\n"+key.toString()+"\t");
					counter = 0;
				}
      }
      if (counter > 1){
				outputval.set(sb.toString());
				context.write (key, outputval);
      }
    }
  }


/**
 * Creates an instance of this tool.
 */
  public ComputeSimilarity (){}

  private static int printUsage (){
    System.out.println ("usage: [input-path] [output-path] [num-reducers]");
    ToolRunner.printGenericCommandUsage (System.out);
    return -1;
  }

/**
 * Runs this tool.
 */
  public int run (String[]args) throws Exception
  {
    if (args.length != 3)
      {
	printUsage ();
	return -1;
      }

    String inputPath = args[0];
    String outputPath = args[1];
    int reduceTasks = Integer.parseInt(args[3]);
    Integer windowSize = Integer.parseInt(args[2]);
    

    sLogger.info ("Tool: ComputeSimilarity");
    sLogger.info (" - input path: " + inputPath);
    sLogger.info (" - output path: " + outputPath);
    sLogger.info (" - number of reducers: " + reduceTasks);

    Configuration conf = new Configuration ();
    conf.set("WindowSize", windowSize.toString());
    Job job = new Job (conf, "ComputeSimilarity");
    job.setJarByClass (ComputeSimilarity.class);

    job.setNumReduceTasks (reduceTasks);

    FileInputFormat.setInputPaths (job, new Path (inputPath));
    FileOutputFormat.setOutputPath (job, new Path (outputPath));

    job.setOutputKeyClass (Text.class);
    job.setOutputValueClass (Text.class);

    job.setMapperClass (MyMapper.class);
    //job.setCombinerClass(MyCombiner.class );
    job.setReducerClass (MyReducer.class);

    // Delete the output directory if it exists already
    Path outputDir = new Path (outputPath);
    FileSystem.get (conf).delete (outputDir, true);

    long startTime = System.currentTimeMillis ();
    job.waitForCompletion (true);
    sLogger.info ("Job Finished in " +
		  (System.currentTimeMillis () - startTime) / 1000.0 +
		  " seconds");

    return 0;
  }

/**
 * Dispatches command-line arguments to the tool via the
 * <code>ToolRunner</code>.
 */
  public static void main (String[]args) throws Exception
  {
    int res =
      ToolRunner.run (new Configuration (), new ComputeSimilarity (), args);

      System.exit (res);
  }
}

