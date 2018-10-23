// https://searchcode.com/api/result/74993087/

/*
 * Cloud9: A MapReduce Library for Hadoop
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you
 * may not use this file except in compliance with the License. You may
 * obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package edu.umd.cloud9.collection.medline;

import java.io.IOException;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import edu.uiowa.icts.medline.Citation;
import edu.uiowa.icts.util.Combinations;
import edu.uiowa.icts.util.XmlInputFormat;

/**
 * <p>
 * Simple demo program that counts all the documents in a collection of MEDLINE citations. This
 * provides a skeleton for MapReduce programs to process the collection. The program takes three
 * command-line arguments:
 * </p>
 *
 * <ul>
 * <li>[input] path to the document collection
 * <li>[output-dir] path to the output directory
 * <li>[mappings-file] path to the docno mappings file
 * </ul>
 *
 * <p>
 * Here's a sample invocation:
 * </p>
 *
 * <blockquote><pre>
 * setenv HADOOP_CLASSPATH "/foo/cloud9-x.y.z.jar:/foo/guava-r09.jar"
 *
 * hadoop jar cloud9-x.y.z.jar edu.umd.cloud9.collection.trec.DemoCountTrecDocuments2 \
 *   -libjars=guava-r09.jar \
 *   /shared/collections/trec/trec4-5_noCRFR.xml \
 *   /user/jimmylin/count-tmp \
 *   /user/jimmylin/docno-mapping.dat
 * </pre></blockquote>
 *
 * @author Jimmy Lin
 */
public class DemoCountMedlineCitations extends Configured implements Tool {
  private static final Logger LOG = Logger.getLogger(DemoCountMedlineCitations.class);
  private static enum Count { DOCS };

  
  public static class ReducerToFilesystem extends Reducer<Text, IntWritable, Text, IntWritable>
  {
      public void reduce(Text key, Iterable<IntWritable> values, Context context) throws IOException, InterruptedException
      {
          int sum = 0;
          for (IntWritable val : values)
              sum += val.get();
          context.write(key, new IntWritable(sum));
      }
  }
  
  private static class MyMapper extends Mapper<LongWritable, Text, Text, IntWritable> {
    private static final Text docid = new Text();
    private static final IntWritable val = new IntWritable(1);
   // private DocnoMapping docMapping;
    private Text word = new Text();
   
    
	private static JAXBContext jContext;
	private static Unmarshaller unmarshaller;
	
    @Override
    public void setup(Context context) {
      LOG.setLevel(Level.ALL);
      Configuration conf = null;
      conf = context.getConfiguration();
      
      try {
  				jContext=JAXBContext.newInstance("edu.uiowa.medline.xml");
  			} catch (JAXBException e) {
  				// TODO Auto-generated catch block
  				LOG.error("Error Message", e);
  				//e.printStackTrace();
  			} 
  			
  			try {
  				unmarshaller = jContext.createUnmarshaller() ;
  			} catch (JAXBException e) {
  				// TODO Auto-generated catch block
  				LOG.error("Error Message", e);
  				//e.printStackTrace();
  			
  			} 
    
      
      
//      Path[] localFiles = null;
//      try {
//        conf = context.getConfiguration();
//        localFiles = DistributedCache.getLocalCacheFiles(conf);
//
//        // Instead of hard-coding the actual concrete DocnoMapping class, have the name of the
//        // class passed in as a property; this makes the mapper more general.
//        docMapping = (DocnoMapping) Class.forName(conf.get("DocnoMappingClass")).newInstance();
//       LOG.info("Running Setup");
////        MedlineDocnoMapping mdm = new MedlineDocnoMapping();
////        docMapping = (DocnoMapping) mdm;
//        LOG.info("Got Docno Mapping");
//      } catch (Exception e) {
//        throw new RuntimeException("Error initializing DocnoMapping!");
//      }
//      try {
//        // Simply assume that the mappings file is the only file in the distributed cache.
//        docMapping.loadMapping(localFiles[0], FileSystem.getLocal(conf));
//      }catch (IOException e) {
//        System.out.println(e.getMessage());
//      }
//      catch (Exception e) {
//       LOG.error("Error initalizing DocnoMapping!",e);
//        throw new RuntimeException("Error initializing DocnoMapping!");
//      }
    }

    @Override
    public void map(LongWritable key, Text doc, Context context)
        throws IOException, InterruptedException {
      context.getCounter(Count.DOCS).increment(1);
      int start = Integer.parseInt(context.getConfiguration().get("start"));
      int end = Integer.parseInt(context.getConfiguration().get("end"));
      //LOG.info("xml: " + doc.toString().substring(0, 20));	
      Citation cit = new Citation(doc.toString(), unmarshaller);
      //LOG.info("Title:"  + cit.getArticleTitle());
      	
		List<String> terms = cit.getMinorMeshHeading();
		if (context.getCounter(Count.DOCS).getValue() % 1000 == 0) {
			LOG.info("Current Count: " + context.getCounter(Count.DOCS).getValue());
		}
		if (terms.size() > 2 ) {
//			LOG.info("Title:"  + cit.getArticleTitle());
//			for (String term : terms) {
//				LOG.info("Terms: " + "[" +  cit.getPmid() + "]" + term);
//			}
			Combinations c = new Combinations();
			List<String> combinations = c.getCombinations(terms,start,end);
			for (String term : combinations) {
	//			LOG.info("Terms: " + "[" +  cit.getPmid() + "]" + term);
	
				word.set( term);
				context.write(word, val);
			}
		//	LOG.info("Doc ID: " + cit.getPmid());
		}
	     
    }
  }

  /**
   * Creates an instance of this tool.
   */
  public DemoCountMedlineCitations() {
  }

  private static int printUsage() {
    System.out.println("usage: [input] [output-dir] [mappings-file]");
    ToolRunner.printGenericCommandUsage(System.out);
    return -1;
  }

  /**
   * Runs this tool.
   */
  public int run(String[] args) throws Exception {
//    if (args.length != 2) {
//      printUsage();
//      return -1;
//    }

    String inputPath = args[0];
    String outputPath = args[1];
    String startVal = args[2];
    String endVal = args[3];
   // String mappingFile = args[2];

    LOG.info("Tool: " + DemoCountMedlineCitations.class.getCanonicalName());
    LOG.info(" - input: " + inputPath);
    LOG.info(" - output dir: " + outputPath);
 //   LOG.info(" - docno mapping file: " + mappingFile);

    Job job = new Job(getConf(), DemoCountMedlineCitations.class.getSimpleName());
    job.setJarByClass(DemoCountMedlineCitations.class);
    job.setNumReduceTasks(12);

    // Pass in the class name as a String; this is makes the mapper general in being able to load
    // any collection of Indexable objects that has docid/docno mapping specified by a DocnoMapping
    // object.
    //job.getConfiguration().set("DocnoMappingClass", MedlineDocnoMapping.class.getCanonicalName());

    // Put the mapping file in the distributed cache so each map worker will have it.
    //DistributedCache.addCacheFile(new URI(mappingFile), job.getConfiguration());

    FileInputFormat.setInputPaths(job, new Path(inputPath));
    FileOutputFormat.setOutputPath(job, new Path(outputPath));
    FileOutputFormat.setCompressOutput(job, false);

    job.getConfiguration().set("xmlinput.start", "<MedlineCitation O");
    job.getConfiguration().set("xmlinput.end", "</MedlineCitation>");
    job.getConfiguration().set("start", startVal);
    job.getConfiguration().set("end", endVal);
    
    job.setInputFormatClass(XmlInputFormat.class);
    job.setOutputKeyClass(Text.class);
    job.setOutputValueClass(IntWritable.class);

    job.setMapperClass(MyMapper.class);
    job.setCombinerClass(ReducerToFilesystem.class);
    job.setReducerClass(ReducerToFilesystem.class);
    
    
    // Delete the output directory if it exists already.
    FileSystem.get(job.getConfiguration()).delete(new Path(outputPath), true);

    job.waitForCompletion(true);

    return 0;
  }

  /**
   * Dispatches command-line arguments to the tool via the {@code ToolRunner}.
   */
  public static void main(String[] args) throws Exception {
    ToolRunner.run(new Configuration(), new DemoCountMedlineCitations(), args);
  }
}

