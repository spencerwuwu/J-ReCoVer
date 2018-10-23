// https://searchcode.com/api/result/73347378/

package edu.upenn.mkse212.pennbook.hadoop;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;

public class IterReducer extends Reducer<Text,Text,Text,Text>
{
	//output fromNode		toNodes;weight;node,adsorpvalue, node, adsorp. value...
	//computes sums to get each adsorp. value
	public void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException 
	{
		StringBuffer outValue = new StringBuffer();
		HashMap<String,Double> ranks = new HashMap<String,Double>();
		//sum calculated for each node that has an adsoprtion value
		//key is the node and value is the adsorption value
		
		String weight="";
		StringBuffer links = new StringBuffer();
		for (Text v : values)
		{
			String[] parts = v.toString().split(";");
			if (parts[0].equals("IN")) {
				double inWeight = Double.parseDouble(parts[1]);
				String[] nodesRanks = new String[0];
				if (parts.length>2)
					nodesRanks = parts[2].split(",");
				for (int i=0; i<nodesRanks.length-1; i+=2)
				{
					String node = nodesRanks[i];
					//parse value propagated toward this node
					double rank = Double.parseDouble(nodesRanks[i+1]);
					
					/*add to sum for relevant node, or set the value
					if no value has been added for this node's adsorption value
					value*/
					if (ranks.containsKey(node))
						ranks.put(node, ranks.get(node)+inWeight*rank);
					else
						ranks.put(node, inWeight*rank);
				}
			}
			else if (parts[0].equals("OUT"))
			{
				links.append(parts[1]);
				links.append(",");
				weight = parts[2];
			}
		}
		
		if (links.length()>1)
			links.deleteCharAt(links.length()-1);//delete extra comma
		outValue.append(links);//append destination nodes for future mapping
		outValue.append(";");
		outValue.append(weight);//append weight for values propagated from this node
		outValue.append(";");
		
		for (String node : ranks.keySet())
		{
			outValue.append(node);
			outValue.append(",");
			outValue.append(ranks.get(node));
			outValue.append(",");
		}
		if (outValue.charAt(outValue.length()-1)==',')
			outValue.deleteCharAt(outValue.length()-1);//remove extra comma
		
		context.write(key, new Text( outValue.toString() ));
	}
}

