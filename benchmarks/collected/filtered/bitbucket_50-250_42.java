// https://searchcode.com/api/result/121693720/

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.services.simpledb.AmazonSimpleDB;
import com.amazonaws.services.simpledb.AmazonSimpleDBClient;
import com.amazonaws.services.simpledb.model.Attribute;
import com.amazonaws.services.simpledb.model.BatchPutAttributesRequest;
import com.amazonaws.services.simpledb.model.CreateDomainRequest;
import com.amazonaws.services.simpledb.model.DeleteAttributesRequest;
import com.amazonaws.services.simpledb.model.DeleteDomainRequest;
import com.amazonaws.services.simpledb.model.Item;
import com.amazonaws.services.simpledb.model.PutAttributesRequest;
import com.amazonaws.services.simpledb.model.ReplaceableAttribute;
import com.amazonaws.services.simpledb.model.ReplaceableItem;
import com.amazonaws.services.simpledb.model.SelectRequest;
import com.amazonaws.util.json.JSONArray;
import com.amazonaws.util.json.JSONObject;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.conf.*;
import org.apache.hadoop.io.*;
import org.apache.hadoop.mapred.*;
import org.apache.hadoop.util.*;

public class WifyDB {
	
	public static class Map extends MapReduceBase implements Mapper<LongWritable, Text, Text, IntWritable> {
		private final static IntWritable one = new IntWritable(1);
		private Text word = new Text();
		
		public void map(LongWritable key, Text value, OutputCollector<Text, IntWritable> output, Reporter reporter) throws IOException {
	            String line = value.toString();
		    StringTokenizer tokenizer = new StringTokenizer(line);
		    while (tokenizer.hasMoreTokens()) {
	                word.set(tokenizer.nextToken());
	                output.collect(word, one);
		    }
	        }
	}
	    
	public static class Reduce extends MapReduceBase implements Reducer<Text, IntWritable, Text, IntWritable> {
		public void reduce(Text key, Iterator<IntWritable> values, OutputCollector<Text, IntWritable> output, Reporter reporter) throws IOException {
	            if (key.toString().equals("Haze")) {
	                int sum = 0;
	                while (values.hasNext()) {
	                    sum += values.next().get();
	                }
	                output.collect(key, new IntWritable(sum));
	            }
		}
	}
	
	public static void main(String[] args) throws Exception {
		
		AmazonSimpleDB sdb = new AmazonSimpleDBClient(new PropertiesCredentials(
				WifyDB.class.getResourceAsStream("AwsCredentials.properties")));
		
		try {
			
			//create new domain, if domain exists it doesn't give an error, it doesn't
			//do anything
			String wifyDomain = "WifyDB";
            sdb.createDomain(new CreateDomainRequest(wifyDomain));
			
			//delete domain, if domain doesn't exist it doesn't give an error, it doesn't
			//do anything
			//String delDomain = "WifyDB";
			//sdb.deleteDomain(new DeleteDomainRequest(delDomain));
			
			// get the list of domains
			//for (String domainName : sdb.listDomains().getDomainNames()) {
                //System.out.println("  " + domainName);
            //}
			
			/*
	        // Open the file that is the first 
	        // command line parameter
	        FileInputStream fstream = new FileInputStream("conditions.txt");
			// Get the object of DataInputStream
			DataInputStream in = new DataInputStream(fstream);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			String strLine;
			//Read File Line By Line
			int i = 0;
			while ((strLine = br.readLine()) != null)   {
				
				String[] str = strLine.split("\t");
			
				// put data into the domain
				sdb.batchPutAttributes(new BatchPutAttributesRequest(wifyDomain, 
						createData(i, str[0], str[1], str[2], str[3])));
				i += 1;
			}
			//Close the input stream
			in.close(); 
			*/
			
			
			// user input
            String userCond = "15";
			String userLoc = "Houten";
			
			String selectExpression = "select CityName, Condition from `" + wifyDomain + "` where Temperature = '"+userCond+"'";
			SelectRequest selectRequest = new SelectRequest(selectExpression);
            for (Item item : sdb.select(selectRequest).getItems()) {
                System.out.println();
                System.out.println("	Id: " + item.getName());
                for (Attribute attribute : item.getAttributes()) {
                    System.out.println(attribute.getName()+": " + attribute.getValue());
                }
            }
			
			
		} catch (AmazonServiceException ase) {
            System.out.println("Caught an AmazonServiceException, which means your request made it "
                    + "to Amazon SimpleDB, but was rejected with an error response for some reason.");
            System.out.println("Error Message:    " + ase.getMessage());
            System.out.println("HTTP Status Code: " + ase.getStatusCode());
            System.out.println("AWS Error Code:   " + ase.getErrorCode());
            System.out.println("Error Type:       " + ase.getErrorType());
            System.out.println("Request ID:       " + ase.getRequestId());
        } catch (AmazonClientException ace) {
            System.out.println("Caught an AmazonClientException, which means the client encountered "
                    + "a serious internal problem while trying to communicate with SimpleDB, "
                    + "such as not being able to access the network.");
            System.out.println("Error Message: " + ace.getMessage());
		} catch (Exception e) {
        	System.out.println(e.getMessage());
        }
	}
	
	private static List<ReplaceableItem> createData(int rowId, String cityName, String woeid, String cond, String temp) {
        List<ReplaceableItem> sampleData = new ArrayList<ReplaceableItem>();
        
        sampleData.add(new ReplaceableItem(""+rowId+"").withAttributes(
            	new ReplaceableAttribute("CityName", cityName, true),
        		new ReplaceableAttribute("Woeid", woeid, true),
            	new ReplaceableAttribute("Condition", cond, true),
            	new ReplaceableAttribute("Temperature", temp, true)));	
        
        return sampleData;
    }
}
