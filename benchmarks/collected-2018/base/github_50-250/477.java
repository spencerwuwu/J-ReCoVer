// https://searchcode.com/api/result/73347331/

package edu.upenn.mkse212.pennbook.hadoop;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;

import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;

//output node \t list of destinNationNodes;weight;node, adsorption value,node, adsorption value...
//for init, the adsorption value for the current node is set at 1
//weight is set to 1/n, where n is the number of outward edges this node has
public class InitReducer extends Reducer<Text,Text,Text,Text>
{
	public void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException 
	{
		//HashSet<String> links = new HashSet<String>();
		StringBuffer outValue = new StringBuffer();
		int numberOfLinks = 0; //count of number of outward edges for creating weight
		for (Text v : values)
		{
			outValue.append(v.toString());//create list of destination nodes
			outValue.append(",");
			numberOfLinks++;
		}
		outValue.deleteCharAt(outValue.length()-1);//remove extra comma
		
		
		
		outValue.append(";");
		
		double weight=0;
		if (numberOfLinks!=0)
			weight = (float)1/numberOfLinks;
		outValue.append(weight+"");
		outValue.append(";");
		
		//initialize adsorption value corresponding to this node to 1
		String[] keyParts = key.toString().split(":");
		if (keyParts[0].equals("user")) {
			outValue.append(keyParts[1]);
			outValue.append(",");
			outValue.append("1");
		}
		
		
		
		context.write(key, new Text( outValue.toString() ));
		
	}
}

