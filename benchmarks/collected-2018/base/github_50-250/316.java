// https://searchcode.com/api/result/99975964/

import java.io.*;
import java.util.*;
import java.lang.*;

import org.apache.hadoop.fs.*;  
import org.apache.hadoop.filecache.*;
import org.apache.hadoop.conf.*;
import org.apache.hadoop.io.*;
import org.apache.hadoop.mapred.*;
import org.apache.hadoop.util.*;

public class Martin_Liu_Yancey {

    public static class KMapper extends  MapReduceBase implements Mapper <LongWritable, Text, IntWritable, Text> { 

        ArrayList<ArrayList<Double>> centroid;  


        public void configure(JobConf jobconf) {
            try {
                Path[] cacheFiles = DistributedCache.getLocalCacheFiles(jobconf);
                if (cacheFiles != null && cacheFiles.length > 0) { 
                    try {
                        centroid = new ArrayList<ArrayList<Double>>(readList(cacheFiles[0].toString()));
                    } catch (Exception e) {
                        System.err.println("Exception reading files!");
                    }
                }
            } catch (IOException e) {
                System.err.println("Exception reading DistribtuedCache: " + e);
            }
        }


        public void map(LongWritable key, Text value, OutputCollector<IntWritable, Text> output,Reporter reporter) throws IOException { 

            String line = value.toString();
            Text outval = new Text();
            ArrayList<Double> d_line = new ArrayList<Double>();
            StringTokenizer tokenizer = new StringTokenizer(line);

            while (tokenizer.hasMoreTokens()) {   
                String elem = tokenizer.nextToken();
                d_line.add(Double.parseDouble(elem));
            } 

            String outvalstr ="";
            
            for (int i =0; i < d_line.size(); i++) {
                if (i==0) outvalstr = d_line.get(i).toString();
                else  outvalstr  = outvalstr + "," + d_line.get(i);
            }

            double min_distance = Double.MAX_VALUE;
            int temkk = centroid.size();
            int NN_cluster = 0;

            for (int i = 0; i < centroid.size(); i++) {
                double sum = 0;
                double curr_distance = 0;
                for (int j = 0; j < d_line.size(); j++) {
                    sum = sum + (d_line.get(i) - centroid.get(i).get(j))*(d_line.get(i) - centroid.get(i).get(j));
                }
                curr_distance = Math.sqrt(sum);
                if (curr_distance < min_distance) {
                    NN_cluster = i;
                    min_distance = curr_distance;
                }   
            }

            outval.set(outvalstr);
            output.collect(new IntWritable(NN_cluster+1), outval);
        }  
    }  


    public static class KReducer extends MapReduceBase implements Reducer<IntWritable, Text, IntWritable, Text> {
        
        public void reduce(IntWritable key, Iterator<Text> values, OutputCollector<IntWritable,Text> output,
            Reporter reporter) throws IOException {
            
            Text outval = new Text();
            int elm_num = 0;
            ArrayList<Double> init_array = new ArrayList<Double>();
            String line = values.next().toString();
            String[] sArray1 = line.split(",");
            
            for (int i =0; i<sArray1.length; i++) { 
                init_array.add(Double.parseDouble(sArray1[i]));
            }

            while (values.hasNext()) {
                String line1 = values.next().toString();
                String[] sArray = line.split(",");

                for (int i = 0; i < sArray.length; i++) {
                    double sum = init_array.get(i)+Double.parseDouble(sArray[i]);
                    init_array.set(i,sum);
                }
                elm_num++;
            }

            for (int i = 0; i < init_array.size(); i++) {
                double mean = init_array.get(i)/elm_num;
                init_array.set(i,mean);
            }

            String outvalstr = "";

            for (int i =0; i < init_array.size();i++) {
                if (i==0) outvalstr = init_array.get(i).toString();
                else  outvalstr  = outvalstr + " " + init_array.get(i).toString();
            }

            outval.set(outvalstr);
            output.collect(key,outval);
        }       
    }

    public static final String baseHdfsPath = "";
    public static String INITCENTERS_LIST = "/home/huser25/centroids.txt";


    public static ArrayList<ArrayList<Double>> readList(String filename) throws Exception{

        ArrayList<ArrayList<Double>> results = new ArrayList<ArrayList<Double>>();
        
        try {
            BufferedReader cacheReader = new BufferedReader(new FileReader(filename));
            try {
                String line;                    
                while ((line = cacheReader.readLine()) != null) {
                    StringTokenizer tokenizer = new StringTokenizer(line);
                    String c_num = tokenizer.nextToken();
                    ArrayList<Double> temp = new ArrayList<Double>();
                    
                    while (tokenizer.hasMoreTokens()) {
                        String cur_cenx = tokenizer.nextToken();
                        temp.add(Double.parseDouble(cur_cenx));
                    }

                    results.add(temp);
                }
            } finally {
                cacheReader.close();
                return results;
            }

        } catch (IOException e) {
            System.err.println("Exceptione: " + e);
        }

        return results;
    }


    public static void main(String[] args) throws Exception {

        Path inputPath = new Path(baseHdfsPath + args[0]);
        String outDir = baseHdfsPath + args[1];
        int numIter = Integer.valueOf(args[2]);

        for (int iter = 0; iter < numIter; iter++) {

            Path centerPath = new Path(outDir+"/iter"+String.format("%03d",iter)+"/centroid"); // Previous output
            Path outputPath = new Path(outDir+"/iter"+String.format("%03d",iter+1));
            Configuration conf = new Configuration();
            JobConf jobconf = new JobConf(conf, Martin_Liu_Yancey.class);
            jobconf.setJobName("Kmeans");
            FileSystem fs =FileSystem.get(jobconf);

            if (iter == 0) {
                fs.copyFromLocalFile(false, true, new Path(INITCENTERS_LIST),centerPath);
            }

            jobconf.setOutputKeyClass(IntWritable.class);
            jobconf.setOutputValueClass(Text.class);
            jobconf.setMapperClass(KMapper.class);
            jobconf.setReducerClass(KReducer.class);
            jobconf.setInputFormat(TextInputFormat.class);
            jobconf.setOutputFormat(TextOutputFormat.class);

            FileInputFormat.setInputPaths(jobconf, inputPath);
            FileOutputFormat.setOutputPath(jobconf, outputPath);
            DistributedCache.addCacheFile(centerPath.toUri(),jobconf);
            JobClient.runJob(jobconf);
            Path newcenterPath = new Path(outDir+"/iter"+String.format("%03d",iter+1)+"/centroid");
            FileUtil.copyMerge(fs, outputPath, fs, newcenterPath, false, conf, null);
        }
    }
}
